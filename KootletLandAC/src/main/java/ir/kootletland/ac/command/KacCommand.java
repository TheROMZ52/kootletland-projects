package ir.kootletland.ac.command;

import ir.kootletland.ac.engine.AntiCheatEngine;
import ir.kootletland.ac.integration.Integration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public final class KacCommand implements CommandExecutor, TabCompleter {
    private final Plugin plugin;
    private final AntiCheatEngine engine;
    public KacCommand(Plugin plugin,AntiCheatEngine engine){this.plugin=plugin;this.engine=engine;}

    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!s.hasPermission("kootletlandac.admin")){s.sendMessage("§cNo permission.");return true;}
        if(a.length==0){s.sendMessage("§e/kac debug <player> §7| §e/kac verbose <player> §7| §e/kac stats §7| §e/kac checks §7| §e/kac integrations §7| §e/kac profile [reset] §7| §e/kac history <player> §7| §e/kac alerts §7| §e/kac reload");return true;}
        switch(a[0].toLowerCase()){
            case "debug","verbose" -> {
                if(a.length<2){s.sendMessage("§cPlayer required.");return true;}
                Player p=Bukkit.getPlayerExact(a[1]);
                if(p==null){s.sendMessage("§cPlayer not found.");return true;}
                engine.debug(p).forEach(s::sendMessage);
                if(a[0].equalsIgnoreCase("verbose")&&engine.get(p.getUniqueId())!=null){
                    engine.get(p.getUniqueId()).evidence().stream().limit(8).forEach(e->s.sendMessage("§8- §e"+e.check()+" §7conf="+String.format("%.1f%%",e.confidence()*100)+" §7vl="+String.format("%.2f",e.violation())+" §7"+e.data()));
                }
            }
            case "stats" -> engine.stats().forEach(s::sendMessage);
            case "profile" -> {
                if(a.length>1&&a[1].equalsIgnoreCase("reset")){engine.profiler().reset();s.sendMessage("§aProfiler reset.");}
                else{
                    s.sendMessage("§eProfiler §7(section | calls | avg | max | total)"+(engine.profiler().enabled()?"":" §c[disabled in config]"));
                    engine.profiler().report().forEach(x->s.sendMessage("§7"+x.name()+" §f"+x.calls()+" §7| §f"+String.format("%.2fµs",x.avgMicros())+" §7| §f"+String.format("%.0fµs",x.maxMicros())+" §7| §f"+String.format("%.1fms",x.totalMillis())));
                }
            }
            case "history" -> {
                if(a.length<2){s.sendMessage("§cPlayer required.");return true;}
                Player online=Bukkit.getPlayerExact(a[1]);
                java.util.UUID id=online!=null?online.getUniqueId():(Bukkit.getOfflinePlayerIfCached(a[1])==null?null:Bukkit.getOfflinePlayerIfCached(a[1]).getUniqueId());
                if(id==null){s.sendMessage("§cUnknown player.");return true;}
                if("none".equals(engine.storage().name())){s.sendMessage("§cStorage is off (storage.type in config.yml).");return true;}
                Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
                    var rows=engine.storage().recent(id,10);
                    Bukkit.getScheduler().runTask(plugin,()->{
                        if(rows.isEmpty())s.sendMessage("§7No stored evidence.");
                        rows.forEach(r->s.sendMessage("§8- §e"+r.check()+" §7conf="+String.format("%.1f%%",r.confidence()*100)+" vl="+String.format("%.2f",r.violation())+" §8"+new java.util.Date(r.createdAtMillis())));
                    });
                });
            }
            case "checks" -> s.sendMessage("§eEnabled checks: §f"+engine.enabledChecks()+(engine.customChecks().isEmpty()?"":" §7| custom: "+engine.customChecks().stream().map(x->x.name()).toList()));
            case "integrations" -> {
                for(Integration i:engine.integrations())s.sendMessage("§7"+i.name()+": §f"+(i.available()?"available":"not available"));
            }
            case "alerts" -> s.sendMessage("§eKootletAC §7alerts are "+(plugin.getConfig().getBoolean("settings.alerts")?"enabled":"disabled")+". Automatic punishment: "+(plugin.getConfig().getBoolean("settings.auto-punishment",false)?"§cENABLED":"§aOFF"));
            case "reload" -> {plugin.reloadConfig();engine.reloadChecks();s.sendMessage("§aKootletAC configuration reloaded without resetting player state.");}
            default -> s.sendMessage("§cUnknown subcommand.");
        }
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender s,Command c,String a,String[] args){
        if(args.length==1)return List.of("debug","verbose","stats","profile","history","checks","integrations","alerts","reload");
        if(args.length==2&&(args[0].equalsIgnoreCase("debug")||args[0].equalsIgnoreCase("verbose")||args[0].equalsIgnoreCase("history")))return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        return List.of();
    }
}
