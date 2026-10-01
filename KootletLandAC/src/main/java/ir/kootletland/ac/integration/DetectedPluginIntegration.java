package ir.kootletland.ac.integration;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * LiteBans and LuckPerms need no code of their own:
 * punishment commands run through the console (LiteBans records /tempban, /kick, ...), and the bypass and alert
 * permissions are plain Bukkit permission nodes that LuckPerms resolves. This class only reports what was found.
 */
public final class DetectedPluginIntegration implements Integration {
    private final Plugin owner;
    private final String pluginName;
    private final String note;

    public DetectedPluginIntegration(Plugin owner,String pluginName,String note){
        this.owner=owner;
        this.pluginName=pluginName;
        this.note=note;
    }

    @Override public String name(){return pluginName;}
    @Override public boolean available(){return Bukkit.getPluginManager().isPluginEnabled(pluginName);}
    @Override public void start(){
        Plugin p=Bukkit.getPluginManager().getPlugin(pluginName);
        owner.getLogger().info(pluginName+" "+(p==null?"":p.getDescription().getVersion())+" detected: "+note);
    }
    @Override public void stop(){}
}
