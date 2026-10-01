package ir.kootletland.ac.integration;

import ir.kootletland.ac.engine.AntiCheatEngine;
import ir.kootletland.ac.model.Evidence;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Map;

/**
 * Optional GrimAC bridge. Grim flags are consumed as ONE signal; they never punish on their own
 * (the correlation engine only counts them next to our own repeated evidence).
 *
 * Uses reflection so KootletLandAC has no compile-time dependency on Grim and keeps working without it.
 * Written against the public GrimAPI 1.3 event bus documentation; NOT verified against a live Grim server yet.
 * If anything does not match, the integration disables itself and logs why.
 */
public final class GrimIntegration implements Integration {
    private static final String[] API_CLASSES={"ac.grim.grimac.api.GrimAbstractAPI"};
    private static final String[] FLAG_CLASSES={"ac.grim.grimac.api.event.events.FlagEvent","ac.grim.grimac.api.events.FlagEvent"};

    private final Plugin plugin;
    private final AntiCheatEngine engine;
    private Object bus;
    private boolean running;
    private String status="not started";

    public GrimIntegration(Plugin plugin,AntiCheatEngine engine){this.plugin=plugin;this.engine=engine;}

    @Override public String name(){return "GrimAC";}

    @Override public boolean available(){
        return plugin.getConfig().getBoolean("integrations.grim",true)&&Bukkit.getPluginManager().isPluginEnabled("GrimAC");
    }

    public String status(){return status;}

    @Override public void start(){
        if(running)return;
        try{
            Class<?> apiClass=firstClass(API_CLASSES);
            Class<?> flagClass=firstClass(FLAG_CLASSES);
            if(apiClass==null||flagClass==null)throw new IllegalStateException("Grim API classes not found (unsupported Grim version?)");
            @SuppressWarnings("unchecked")
            RegisteredServiceProvider<Object> registration=Bukkit.getServicesManager().getRegistration((Class<Object>)apiClass);
            if(registration==null)throw new IllegalStateException("Grim API service not registered");
            Object api=registration.getProvider();
            bus=apiClass.getMethod("getEventBus").invoke(api);
            Object channel=find(bus,"get",1).invoke(bus,flagClass);

            Method subscribe=null;
            for(Method m:channel.getClass().getMethods()){
                if(m.getName().equals("onFlag")&&m.getParameterCount()==2&&m.getParameterTypes()[0]==Object.class){subscribe=m;break;}
            }
            if(subscribe==null)throw new IllegalStateException("FlagEvent channel has no onFlag(Object, handler)");
            subscribe.setAccessible(true);
            Class<?> handlerType=subscribe.getParameterTypes()[1];
            Object handler=Proxy.newProxyInstance(handlerType.getClassLoader(),new Class<?>[]{handlerType},flagHandler());
            subscribe.invoke(channel,plugin,handler);
            running=true;
            status="active";
            plugin.getLogger().info("GrimAC integration active (flags are used as one signal, never as proof).");
        }catch(Throwable t){
            status="inactive: "+t.getClass().getSimpleName()+": "+t.getMessage();
            plugin.getLogger().warning("GrimAC integration disabled itself: "+status);
        }
    }

    @Override public void stop(){
        if(!running||bus==null)return;
        try{
            Method m=find(bus,"unregisterAllListeners",1);
            m.invoke(bus,plugin);
        }catch(Throwable ignored){
        }
        running=false;
        status="stopped";
    }

    private InvocationHandler flagHandler(){
        return (proxy,method,args)->{
            if(method.getDeclaringClass()==Object.class){
                return switch(method.getName()){
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy==args[0];
                    default -> "KootletLandAC Grim flag handler";
                };
            }
            try{
                if(args!=null&&args.length>=3)onFlag(args[0],args[1],args[2]);
            }catch(Throwable ignored){
            }
            // the handler contract returns the cancelled state; we only observe, so pass it through
            if(method.getReturnType()==boolean.class)return args!=null&&args.length>=4&&args[3] instanceof Boolean b&&b;
            return null;
        };
    }

    private void onFlag(Object user,Object check,Object verbose)throws Exception{
        String playerName=String.valueOf(find(user,"getName",0).invoke(user));
        String checkName=String.valueOf(find(check,"getCheckName",0).invoke(check));
        Player player=Bukkit.getPlayerExact(playerName);
        if(player==null)return;
        double confidence=Math.max(.3,Math.min(.8,plugin.getConfig().getDouble("integrations.grim-confidence",.5)));
        String detail=String.valueOf(verbose);
        if(detail.length()>120)detail=detail.substring(0,120);
        engine.submit(player.getUniqueId(),new Evidence(player.getName(),"Grim",confidence,.04,Instant.now(),Map.of("grimCheck",checkName,"verbose",detail)));
    }

    private static Class<?> firstClass(String[] names){
        for(String name:names){
            try{return Class.forName(name);}catch(ClassNotFoundException ignored){}
        }
        return null;
    }

    private static Method find(Object target,String name,int params)throws NoSuchMethodException{
        for(Method m:target.getClass().getMethods()){
            if(m.getName().equals(name)&&m.getParameterCount()==params){m.setAccessible(true);return m;}
        }
        throw new NoSuchMethodException(target.getClass().getName()+"#"+name);
    }
}
