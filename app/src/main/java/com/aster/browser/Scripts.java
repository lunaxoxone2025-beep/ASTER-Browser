package com.aster.browser;

import org.json.JSONObject;

/** JavaScript snippets injected into pages. All are best-effort and wrapped in try/catch. */
final class Scripts {
    private Scripts() { }

    static String quote(String s) { return JSONObject.quote(s == null ? "" : s); }

    /** Adds (or replaces) a style element with the given CSS. */
    static String style(String id, String css) {
        return "(function(){try{var id=" + quote(id) + ";var s=document.getElementById(id);"
            + "if(!s){s=document.createElement('style');s.id=id;(document.head||document.documentElement).appendChild(s);}"
            + "s.textContent=" + quote(css) + ";}catch(e){}})();";
    }

    static String removeStyle(String id) {
        return "(function(){try{var s=document.getElementById(" + quote(id) + ");if(s)s.remove();}catch(e){}})();";
    }

    /** YouTube: hide ad UI, fast-forward in-stream ads that slip through and press Skip. */
    static String youtube() {
        return "(function(){try{if(window.__asterYt)return;window.__asterYt=1;"
            + "var css='ytd-ad-slot-renderer,ytd-display-ad-renderer,ytd-promoted-sparkles-web-renderer,ytd-in-feed-ad-layout-renderer,"
            + "ytd-banner-promo-renderer,ytd-action-companion-ad-renderer,ytd-promoted-video-renderer,ytd-companion-slot-renderer,"
            + "ytm-promoted-sparkles-web-renderer,ytm-companion-ad-renderer,ytm-promoted-video-renderer,ad-slot-renderer,"
            + "#player-ads,#masthead-ad,.ytp-ad-overlay-container,.ytp-ad-text-overlay,.ytp-ad-image-overlay,"
            + "ytm-paid-content-overlay-renderer,.ytd-merch-shelf-renderer{display:none!important}';"
            + "var st=document.createElement('style');st.textContent=css;(document.head||document.documentElement).appendChild(st);"
            + "var tick=function(){try{"
            + "var p=document.querySelector('.html5-video-player,#movie_player');"
            + "var v=document.querySelector('video');"
            + "var ad=p&&(p.classList.contains('ad-showing')||p.classList.contains('ad-interrupting'));"
            + "if(ad&&v){v.muted=true;if(isFinite(v.duration)&&v.duration>0)v.currentTime=Math.max(v.currentTime,v.duration-0.1);v.playbackRate=16;}"
            + "else if(v&&v.playbackRate===16){v.playbackRate=1;v.muted=false;}"
            + "document.querySelectorAll('.ytp-ad-skip-button,.ytp-ad-skip-button-modern,.ytp-skip-ad-button,.ytm-skip-ad-button,button[class*=skip-ad]').forEach(function(b){try{b.click();}catch(e){}});"
            + "document.querySelectorAll('ytd-ad-slot-renderer,ytm-promoted-sparkles-web-renderer,ad-slot-renderer').forEach(function(e){e.remove();});"
            + "}catch(e){}};setInterval(tick,400);tick();"
            + "}catch(e){}})();";
    }

    /**
     * Runs as early as possible on YouTube: strips ad descriptors from player responses
     * (the same idea as uBlock Origin's json-prune for adPlacements/playerAds).
     */
    static String youtubeEarly() {
        return "(function(){try{if(window.__asterYtE)return;window.__asterYtE=1;"
            + "var K=['adPlacements','playerAds','adSlots','adBreakHeartbeatParams'];"
            + "function prune(o){try{if(o&&typeof o==='object'){for(var i=0;i<K.length;i++){if(K[i] in o)delete o[K[i]];}"
            + "if(o.playerResponse&&typeof o.playerResponse==='object')prune(o.playerResponse);"
            + "if(o.response&&typeof o.response==='object'&&o.response.playerResponse)prune(o.response.playerResponse);}}catch(e){}return o;}"
            + "var jp=JSON.parse;JSON.parse=function(){return prune(jp.apply(this,arguments));};"
            + "if(window.Response&&Response.prototype.json){var rj=Response.prototype.json;Response.prototype.json=function(){return rj.apply(this,arguments).then(prune);};}"
            + "var cur=window.ytInitialPlayerResponse;if(cur)prune(cur);"
            + "try{Object.defineProperty(window,'ytInitialPlayerResponse',{configurable:true,get:function(){return cur;},set:function(v){cur=prune(v);}});}catch(e){}"
            + "}catch(e){}})();";
    }

    /** Fingerprinting farbling: small noise on canvas/audio, generic hardware values and GPC. */
    static String fingerprint(int seed) {
        return "(function(){try{if(window.__asterFp)return;window.__asterFp=1;var seed=" + seed + ";"
            + "function rnd(i){var x=Math.sin(seed+i)*10000;return x-Math.floor(x);}"
            + "try{Object.defineProperty(navigator,'hardwareConcurrency',{get:function(){return 4+(seed%2)*4;}});}catch(e){}"
            + "try{Object.defineProperty(navigator,'deviceMemory',{get:function(){return 8;}});}catch(e){}"
            + "try{Object.defineProperty(navigator,'globalPrivacyControl',{get:function(){return true;}});}catch(e){}"
            + "try{Object.defineProperty(navigator,'doNotTrack',{get:function(){return '1';}});}catch(e){}"
            + "var gid=CanvasRenderingContext2D.prototype.getImageData;"
            + "CanvasRenderingContext2D.prototype.getImageData=function(){var d=gid.apply(this,arguments);"
            + "try{for(var i=0;i<d.data.length;i+=97){d.data[i]=d.data[i]^(rnd(i)>0.5?1:0);}}catch(e){}return d;};"
            + "var tdu=HTMLCanvasElement.prototype.toDataURL;"
            + "HTMLCanvasElement.prototype.toDataURL=function(){try{var c=this.getContext('2d');if(c&&this.width&&this.height){"
            + "var px=c.getImageData(0,0,1,1);c.putImageData(px,0,0);}}catch(e){}return tdu.apply(this,arguments);};"
            + "if(window.AudioBuffer){var gcd=AudioBuffer.prototype.getChannelData;AudioBuffer.prototype.getChannelData=function(){"
            + "var r=gcd.apply(this,arguments);try{for(var i=0;i<r.length;i+=211){r[i]=r[i]+(rnd(i)-0.5)*1e-7;}}catch(e){}return r;};}"
            + "var patch=function(proto){if(!proto)return;var gp=proto.getParameter;proto.getParameter=function(p){"
            + "if(p===37445)return 'Google Inc.';if(p===37446)return 'ANGLE (Generic GPU)';return gp.apply(this,arguments);};};"
            + "if(window.WebGLRenderingContext)patch(WebGLRenderingContext.prototype);"
            + "if(window.WebGL2RenderingContext)patch(WebGL2RenderingContext.prototype);"
            + "}catch(e){}})();";
    }

    /** Keeps media playing when the app is in the background (page sees itself as visible). */
    static String backgroundPlay() {
        return "(function(){try{if(window.__asterBg)return;window.__asterBg=1;"
            + "Object.defineProperty(document,'hidden',{get:function(){return false;}});"
            + "Object.defineProperty(document,'visibilityState',{get:function(){return 'visible';}});"
            + "document.addEventListener('visibilitychange',function(e){e.stopImmediatePropagation();},true);"
            + "window.addEventListener('pagehide',function(e){e.stopImmediatePropagation();},true);"
            + "}catch(e){}})();";
    }

    /** CSS-based night mode for Android versions without WebView algorithmic darkening. */
    static String nightCss() {
        return "html{filter:invert(.92) hue-rotate(180deg)!important;background:#111!important}"
            + "img,video,picture,canvas,iframe,svg image,[style*=\"background-image\"]{filter:invert(1) hue-rotate(180deg)!important}";
    }

    /** Extracts article blocks as JSON: {title, site, blocks:[{t:'h'|'p'|'img'|'li'|'q', v:'...'}]}. */
    static String readerExtract() {
        return "(function(){try{"
            + "var cands=[].slice.call(document.querySelectorAll('article,main,[role=main],.post,.article,.entry-content,.post-content,#content,.content,body'));"
            + "var best=document.body,score=0;cands.forEach(function(c){var s=0;c.querySelectorAll('p').forEach(function(p){var l=(p.innerText||'').trim().length;if(l>60)s+=l;});if(s>score){score=s;best=c;}});"
            + "var out=[];var seen=0;"
            + "best.querySelectorAll('h1,h2,h3,p,li,blockquote,img,pre').forEach(function(e){if(seen>600)return;"
            + "if(e.closest('nav,footer,aside,form,header,[aria-hidden=true],.ad,.ads,.advert,.share,.related,.comments'))return;"
            + "var tag=e.tagName.toLowerCase();"
            + "if(tag==='img'){var src=e.currentSrc||e.src||'';if(src.indexOf('http')===0&&(e.naturalWidth||e.width||0)>160){out.push({t:'img',v:src});seen++;}return;}"
            + "var txt=(e.innerText||'').replace(/\\s+/g,' ').trim();if(!txt)return;"
            + "if(tag==='p'&&txt.length<25)return;if(tag==='li'&&(txt.length<20||e.closest('p')))return;"
            + "var t=tag[0]==='h'?'h':(tag==='li'?'li':(tag==='blockquote'?'q':(tag==='pre'?'pre':'p')));"
            + "out.push({t:t,v:txt.slice(0,6000)});seen++;});"
            + "var h1=document.querySelector('h1');"
            + "return JSON.stringify({title:(h1&&h1.innerText.trim())||document.title||location.hostname,site:location.hostname,url:location.href,blocks:out,chars:score});"
            + "}catch(e){return JSON.stringify({blocks:[],chars:0});}})()";
    }

    /** Reads the page's theme-color meta tag (used to tint the toolbar). */
    static String themeColor() {
        return "(function(){try{var m=document.querySelector('meta[name=theme-color]');return m?m.content:'';}catch(e){return '';}})()";
    }
}
