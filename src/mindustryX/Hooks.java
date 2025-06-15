package mindustryX;

import arc.*;
import arc.files.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustryX.features.*;
import mindustryX.features.Settings;
import mindustryX.features.ui.*;

import java.net.*;
import java.util.*;

public class Hooks implements ApplicationListener{
    /** invoke before `Vars.init`. Note that may be executed from `Vars.loadAsync` */
    public static void beforeInit(){
        Log.infoTag("MindustryX", "Hooks.beforeInit");
        registerBundle();
        Settings.addSettings();
        SettingsV2.INSTANCE.init();
        DebugUtil.init();//this is safe, and better at beforeInit,
        BindingExt.init();
    }

    /** invoke after loading, just before `Mod::init` */
    @Override
    public void init(){
        Log.infoTag("MindustryX", "Hooks.init");
        LogicExt.init();
        if(AutoUpdate.INSTANCE.getActive())
            AutoUpdate.INSTANCE.checkUpdate();
        if(!Vars.headless){
            RenderExt.init();
            TimeControl.init();
            UIExt.init();
            ReplayController.init();
            ArcOld.colorizeContent();
            DamagePopup.init();
        }
        if(Core.settings.getBool("console")){
            Vars.mods.getScripts().runConsole("X=Packages.mindustryX.features");
        }
    }

    @SuppressWarnings("unused")//call before arc.util.Http$HttpRequest.block
    public static void onHttp(Http.HttpRequest req){
        if(VarsX.githubMirror.get()){
            try{
                String url = req.url;
                String host = new URL(url).getHost();
                if(host.contains("github.com") || host.contains("raw.githubusercontent.com")){
                    url = "https://gh.tinylake.top/" + url;
                    req.url = url;
                }
            }catch(Exception e){
                //ignore
            }
        }
    }

    public static @Nullable String onHandleSendMessage(String message, @Nullable Player sender){
        if(message == null) return null;
        if(Vars.ui != null){
            if(!MarkerType.resolveMessage(message))
                try{
                    ArcMessageDialog.resolveMsg(message, sender);
                }catch(Exception e){
                    Log.err(e);
                }
            if(sender != null){
                StringBuilder builder = new StringBuilder();
                if(Vars.state.rules.pvp){
                    builder.append("[#").append(sender.team().color).append("]");
                    builder.append(sender.team() == Vars.player.team() ? Iconc.players + " " : Iconc.units);
                    builder.append("[]");
                }
                builder.append(sender.dead() ? Iconc.alphaaaa : sender.unit().type.emoji());
                builder.append(" ").append(message);
                message = builder.toString();
            }
        }
        return message;
    }

    @Override
    public void update(){
        if(!Vars.headless){
            updateTitle();
            BindingExt.pollKeys();
            if(BindingExt.oreAdsorption.keyDown()) ArcOld.doOreAdsorption();
        }
    }

    private static void registerBundle(){
        //MDTX: bundle overwrite
        try{
            I18NBundle originBundle = Core.bundle;
            Fi handle = Core.files.internal("bundles/bundle-mdtx");
            Core.bundle = I18NBundle.createBundle(handle, Locale.getDefault());
            Reflect.set(Core.bundle, "locale", originBundle.getLocale());
            Log.info("MDTX: bundle has been loaded.");
            var rootBundle = Core.bundle;
            while(rootBundle.getParent() != null){
                rootBundle = rootBundle.getParent();
            }
            Reflect.set(rootBundle, "parent", originBundle);
        }catch(Throwable e){
            Log.err(e);
        }
    }

    private static String lastTitle;

    private void updateTitle(){
        if(Core.graphics == null) return;
        var mod = Vars.mods.orderedMods();
        var title = "MindustryX | 版本号 " + VarsX.version +
        " | mod启用" + mod.count(Mods.LoadedMod::enabled) + "/" + mod.size +
        " | " + Core.graphics.getWidth() + "x" + Core.graphics.getHeight();
        if(!title.equals(lastTitle)){
            lastTitle = title;
            Core.graphics.setTitle(title);
        }
    }
}
