package com.aster.browser;

import org.json.JSONObject;

/**
 * HTML for the full Arcade screen: free games by store (Epic, Steam, Xbox, PlayStation, GOG,
 * mobile, DLC) and gaming news by outlet. Data is rendered client-side from a JSON object and
 * can be replaced later through {@code asterUpdate(data)} without reloading.
 */
final class ArcadePage {
    static final String BASE_URL = "https://aster.arcade/";

    private ArcadePage() { }

    private static void put(JSONObject o, String key, String en) {
        try { o.put(key, I18n.t(en)); } catch (Exception ignored) { }
    }

    static String build(Theme t, String dataJson) {
        JSONObject L = new JSONObject();
        put(L, "games", "Free games");
        put(L, "news", "News");
        put(L, "epicNow", "Free now on Epic");
        put(L, "epicSoon", "Coming soon on Epic");
        put(L, "giveaways", "Giveaways");
        put(L, "all", "All");
        put(L, "mobile", "Mobile");
        put(L, "dlc", "DLC & extras");
        put(L, "get", "Get it");
        put(L, "freeUntil", "Free until %s");
        put(L, "from", "Free from %s");
        put(L, "ends", "Ends %s");
        put(L, "noEnd", "No end date");
        put(L, "free", "FREE");
        put(L, "updated", "Updated %s");
        put(L, "loading", "Loading Arcade…");
        put(L, "empty", "Nothing here right now.");
        put(L, "refresh", "Refresh");
        put(L, "credits", "Data: Epic Games Store, GamerPower and each site's RSS feed. Offers can end early — check the store page.");
        String lang = I18n.es ? "es" : "en";
        String safeData = (dataJson == null || dataJson.isEmpty() ? "{}" : dataJson).replace("</", "<\\/");

        String accent = Theme.css(t.accent);
        String accent2 = Theme.css(t.accent2);
        String surface = t.glass ? (t.light ? "rgba(255,255,255,.62)" : "rgba(255,255,255,.10)") : Theme.cssA(t.surface, 0.95f);
        String border = t.glass ? (t.light ? "rgba(255,255,255,.9)" : "rgba(255,255,255,.22)") : Theme.cssA(t.accent, 0.22f);
        String bg = t.glass ? (t.light ? "linear-gradient(160deg,#d6e4ff,#f0e6ff 55%,#d6f5ee)" : "linear-gradient(160deg,#081636,#1a0a34 55%,#04242c)") : Theme.css(t.bg);

        return "<!doctype html><html lang='" + lang + "'><head><meta charset='utf-8'>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'><meta name='color-scheme' content='" + (t.light ? "light" : "dark") + "'>"
            + "<style>"
            + ":root{--a:" + accent + ";--a2:" + accent2 + ";--s:" + surface + ";--b:" + border + ";--t:" + Theme.css(t.text) + ";--m:" + Theme.css(t.muted) + "}"
            + "*{box-sizing:border-box;-webkit-tap-highlight-color:transparent}"
            + "html,body{margin:0;background:" + bg + ";background-attachment:fixed;color:var(--t);font-family:system-ui,Roboto,Arial,sans-serif;overscroll-behavior-y:none}"
            + "main{max-width:900px;margin:0 auto;padding:4px 14px 40px}"
            + ".seg{position:sticky;top:0;z-index:5;display:flex;gap:6px;padding:8px 0 10px;background:inherit}"
            + ".seg button{flex:1;height:42px;border-radius:21px;border:1px solid var(--b);background:var(--s);color:var(--t);font:700 14px system-ui;backdrop-filter:blur(20px);-webkit-backdrop-filter:blur(20px)}"
            + ".seg button.on{background:linear-gradient(135deg,var(--a),var(--a2));color:#fff;border-color:transparent}"
            + "h2{font-size:12px;letter-spacing:2px;text-transform:uppercase;color:var(--m);margin:22px 2px 10px;display:flex;align-items:center;gap:8px}"
            + "h2 .dot{width:8px;height:8px;border-radius:50%;background:var(--a);box-shadow:0 0 10px var(--a)}"
            + ".row{display:flex;gap:12px;overflow-x:auto;scroll-snap-type:x mandatory;padding-bottom:4px}.row::-webkit-scrollbar,.chips::-webkit-scrollbar{display:none}"
            + ".hero{flex:0 0 min(86%,420px);scroll-snap-align:start;border-radius:22px;overflow:hidden;background:var(--s);border:1px solid var(--b);color:var(--t);text-decoration:none;position:relative}"
            + ".hero img{width:100%;aspect-ratio:16/9;object-fit:cover;display:block;background:#0003}"
            + ".hero .in{padding:12px 14px 14px}.hero b{display:block;font-size:17px;line-height:1.25}.hero p{margin:6px 0 0;font-size:12.5px;line-height:1.45;color:var(--m);max-height:36px;overflow:hidden}"
            + ".badge{position:absolute;top:10px;left:10px;padding:5px 10px;border-radius:12px;font:800 11px system-ui;letter-spacing:1px;color:#fff;background:linear-gradient(135deg,var(--a),var(--a2));box-shadow:0 4px 14px rgba(0,0,0,.3)}"
            + ".meta{display:flex;align-items:center;justify-content:space-between;gap:8px;margin-top:10px;font-size:12px;color:var(--m)}"
            + ".was{text-decoration:line-through;opacity:.75}.get{padding:7px 14px;border-radius:14px;background:var(--a);color:#fff;font-weight:800;font-size:12px;white-space:nowrap}"
            + ".small{flex:0 0 200px;scroll-snap-align:start;border-radius:18px;overflow:hidden;background:var(--s);border:1px solid var(--b);color:var(--t);text-decoration:none}"
            + ".small img{width:100%;aspect-ratio:16/9;object-fit:cover;display:block;background:#0003;filter:saturate(.85)}.small div{padding:9px 11px;font-size:12.5px}.small span{display:block;margin-top:3px;font-size:11px;color:var(--a);font-weight:700}"
            + ".chips{display:flex;gap:8px;overflow-x:auto;padding:2px 0 8px}"
            + ".chip{flex:0 0 auto;height:36px;padding:0 14px;border-radius:18px;border:1px solid var(--b);background:var(--s);color:var(--t);font:600 13px system-ui}"
            + ".chip.on{background:var(--a);border-color:var(--a);color:#fff}"
            + ".grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(160px,1fr));gap:12px}"
            + ".card{border-radius:18px;overflow:hidden;background:var(--s);border:1px solid var(--b);color:var(--t);text-decoration:none;display:flex;flex-direction:column}"
            + ".card img{width:100%;aspect-ratio:16/9;object-fit:cover;display:block;background:#0003}"
            + ".card .in{padding:9px 11px 11px;display:flex;flex-direction:column;gap:5px;flex:1}.card b{font-size:13px;line-height:1.3;max-height:34px;overflow:hidden}"
            + ".plat{font-size:10.5px;color:var(--m);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.price{display:flex;gap:6px;align-items:center;font-size:11.5px;margin-top:auto}.price .f{color:var(--a);font-weight:800}"
            + ".news{display:flex;gap:12px;padding:10px;border-radius:18px;background:var(--s);border:1px solid var(--b);color:var(--t);text-decoration:none;margin-bottom:10px}"
            + ".news img{width:112px;height:72px;flex:0 0 112px;border-radius:12px;object-fit:cover;background:#0003}"
            + ".news b{display:block;font-size:14px;line-height:1.32;max-height:56px;overflow:hidden}.news span{display:block;margin-top:6px;font-size:11.5px;color:var(--m)}.news span i{font-style:normal;color:var(--a);font-weight:700}"
            + "@media(min-width:640px){.news img{width:168px;height:96px;flex-basis:168px}}"
            + ".glass .hero,.glass .small,.glass .card,.glass .news,.glass .chip{backdrop-filter:blur(22px) saturate(180%);-webkit-backdrop-filter:blur(22px) saturate(180%);box-shadow:inset 0 1px 0 rgba(255,255,255,.45),0 10px 26px rgba(0,0,0,.18)}"
            + ".empty{color:var(--m);font-size:13px;padding:12px 4px}"
            + ".sk{height:120px;border-radius:18px;background:linear-gradient(90deg,var(--s),rgba(255,255,255,.14),var(--s));background-size:200% 100%;animation:sh 1.2s infinite;margin-bottom:12px}@keyframes sh{to{background-position:-200% 0}}"
            + ".foot{margin-top:26px;text-align:center;color:var(--m);font-size:11px;line-height:1.5}.foot a{display:inline-block;margin-top:10px;padding:9px 18px;border-radius:18px;border:1px solid var(--b);background:var(--s);color:var(--a);font-weight:700;text-decoration:none}"
            + "</style></head><body class='" + (t.glass ? "glass" : "") + "'><main><div class='seg'><button id='tg'></button><button id='tn'></button></div><div id='c'></div>"
            + "<div class='foot'><div id='up'></div><div id='cr'></div><a href='aster://arcade/refresh' id='rf'></a></div></main><script>"
            + "var D=" + safeData + ",L=" + L.toString().replace("</", "<\\/") + ",LANG='" + lang + "',S={tab:'games',store:'all',src:'all'};"
            + SCRIPT
            + "</script></body></html>";
    }

    private static final String SCRIPT =
        "function e(s){return String(s==null?'':s).replace(/[&<>\"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;',\"'\":'&#39;'}[c];});}"
        + "function u(x){return /^https:\\/\\//.test(x||'')?x:'';}"
        + "function f(k,v){return (L[k]||'').replace('%s',v);}"
        + "function day(s){if(!s||s==='N/A')return '';var d=new Date(String(s).replace(' ','T'));if(isNaN(d))return '';return d.toLocaleDateString(LANG,{day:'numeric',month:'short'});}"
        + "function ago(ms){if(!ms)return '';var s=(Date.now()-ms)/1000;try{var r=new Intl.RelativeTimeFormat(LANG,{numeric:'auto'});"
        + "if(s<3600)return r.format(-Math.max(1,Math.round(s/60)),'minute');if(s<86400)return r.format(-Math.round(s/3600),'hour');return r.format(-Math.round(s/86400),'day');}catch(x){return '';}}"
        + "function img(x){var v=u(x);return v?\"<img loading='lazy' src='\"+e(v)+\"' onerror=\\\"this.style.visibility='hidden'\\\">\":\"<img alt=''>\";}"
        + "var STORES=[['all',null],['steam','Steam',['steam']],['epic','Epic',['epic']],['xbox','Xbox',['xbox']],['ps','PlayStation',['ps4','ps5','playstation']],"
        + "['gog','GOG',['gog']],['pc','PC',['pc']],['mobile',null,['android','ios']],['switch','Switch',['switch']],['dlc',null]];"
        + "function matches(g,key){if(key==='all'||key==='dlc')return true;var p=(g.platforms||'').toLowerCase();for(var i=0;i<STORES.length;i++){if(STORES[i][0]===key){var w=STORES[i][2];for(var j=0;j<w.length;j++)if(p.indexOf(w[j])>=0)return true;}}return false;}"
        + "function games(){var h='',ep=D.epic||{};var now=ep.now||[],soon=ep.soon||[];"
        + "if(now.length){h+=\"<h2><span class='dot'></span>\"+e(L.epicNow)+\"</h2><div class='row'>\";now.forEach(function(g){h+=\"<a class='hero' href='\"+e(u(g.url))+\"'>\"+img(g.image)+\"<span class='badge'>\"+e(L.free)+\"</span><div class='in'><b>\"+e(g.title)+\"</b><p>\"+e(g.desc)+\"</p><div class='meta'><span>\"+(g.worth?\"<span class='was'>\"+e(g.worth)+\"</span> · \":'')+e(f('freeUntil',day(g.end)))+\"</span><span class='get'>\"+e(L.get)+\"</span></div></div></a>\";});h+='</div>';}"
        + "if(soon.length){h+='<h2>'+e(L.epicSoon)+\"</h2><div class='row'>\";soon.forEach(function(g){h+=\"<a class='small' href='\"+e(u(g.url))+\"'>\"+img(g.image)+'<div>'+e(g.title)+'<span>'+e(f('from',day(g.start)))+'</span></div></a>';});h+='</div>';}"
        + "h+='<h2>'+e(L.giveaways)+\"</h2><div class='chips'>\";STORES.forEach(function(s){var name=s[0]==='all'?L.all:s[0]==='mobile'?L.mobile:s[0]==='dlc'?L.dlc:s[1];h+=\"<button class='chip\"+(S.store===s[0]?' on':'')+\"' data-s='\"+s[0]+\"'>\"+e(name)+'</button>';});h+='</div>';"
        + "var list=(S.store==='dlc'?D.loot:D.games)||[];var shown=list.filter(function(g){return matches(g,S.store);});"
        + "if(!shown.length)h+=\"<div class='empty'>\"+e(D.fetched?L.empty:L.loading)+'</div>';else{h+=\"<div class='grid'>\";shown.forEach(function(g){var w=g.worth&&g.worth!=='N/A'?\"<span class='was'>\"+e(g.worth)+'</span>':'';var end=day(g.end);"
        + "h+=\"<a class='card' href='\"+e(u(g.url))+\"'>\"+img(g.image)+\"<div class='in'><b>\"+e(g.title)+\"</b><div class='plat'>\"+e(g.platforms)+\"</div><div class='price'><span class='f'>\"+e(L.free)+'</span>'+w+\"</div><div class='plat'>\"+e(end?f('ends',end):L.noEnd)+'</div></div></a>';});h+='</div>';}"
        + "return h;}"
        + "function news(){var n=D.news||[],src=[],h='';n.forEach(function(x){if(src.indexOf(x.source)<0)src.push(x.source);});"
        + "h+=\"<div class='chips' style='margin-top:8px'><button class='chip\"+(S.src==='all'?' on':'')+\"' data-n='all'>\"+e(L.all)+'</button>';src.forEach(function(s){h+=\"<button class='chip\"+(S.src===s?' on':'')+\"' data-n='\"+e(s)+\"'>\"+e(s)+'</button>';});h+='</div>';"
        + "var shown=n.filter(function(x){return S.src==='all'||x.source===S.src;});"
        + "if(!shown.length)return h+(D.fetched?\"<div class='empty'>\"+e(L.empty)+'</div>':\"<div class='sk'></div><div class='sk'></div><div class='sk'></div>\");"
        + "shown.forEach(function(x){h+=\"<a class='news' href='\"+e(u(x.url))+\"'>\"+img(x.image)+'<div><b>'+e(x.title)+'</b><span><i>'+e(x.source)+'</i> · '+e(ago(x.time))+'</span></div></a>';});return h;}"
        + "function render(){var c=document.getElementById('c');var tg=document.getElementById('tg'),tn=document.getElementById('tn');"
        + "tg.textContent='🎮 '+L.games;tn.textContent='📰 '+L.news;tg.className=S.tab==='games'?'on':'';tn.className=S.tab==='news'?'on':'';"
        + "c.innerHTML=S.tab==='games'?games():news();"
        + "document.getElementById('up').textContent=D.fetched?f('updated',ago(D.fetched)):L.loading;"
        + "document.getElementById('cr').textContent=L.credits;document.getElementById('rf').textContent='↻ '+L.refresh;}"
        + "document.addEventListener('click',function(ev){var b=ev.target.closest('button');if(!b)return;"
        + "if(b.id==='tg'){S.tab='games';}else if(b.id==='tn'){S.tab='news';}else if(b.dataset.s){S.store=b.dataset.s;}else if(b.dataset.n){S.src=b.dataset.n;}else return;"
        + "render();if(b.id)window.scrollTo(0,0);});"
        + "window.asterUpdate=function(d){if(d&&typeof d==='object'){D=d;render();}};render();";
}
