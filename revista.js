/* Radio de ruta · Revista (se carga al abrir la sección Revista) */
'use strict';
document.head.insertAdjacentHTML('beforeend', '<style id="rv-css">' + ".rv-bar[hidden]{display:none}\n.rv-sp{flex:1}\n.rv-tab{background:transparent;border:1px solid rgba(255,255,255,.12);color:var(--muted);border-radius:999px;padding:8px 14px}\n.rv-tab.on{background:rgba(255,255,255,.1);color:var(--text);border-color:rgba(255,255,255,.3)}\n.rv-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(230px,1fr));gap:14px;margin-bottom:14px}\n.rv-card{background:rgba(255,255,255,.04);border:1px solid rgba(255,255,255,.07);border-radius:14px;overflow:hidden;cursor:pointer;display:flex;flex-direction:column}\n.rv-card:focus-visible{outline:2px solid var(--line)}\n.rv-card img,.rv-ph{width:100%;aspect-ratio:16/9;object-fit:cover;background:rgba(255,255,255,.05);display:flex;align-items:center;justify-content:center}\n.rv-ph .ic{width:34px;height:34px;opacity:.5}\n.rv-in{padding:10px 14px 14px}\n.rv-card h3{font-family:'Barlow Condensed',sans-serif;font-size:22px;line-height:1.1;margin:0 0 6px}\n.rv-card.hero{grid-column:1/-1}\n.rv-card.hero h3{font-size:32px}\n.rv-sub{color:var(--muted);margin:0 0 8px}\n.rv-m{font-size:13px;color:var(--muted);margin:0}\n.rv-head{display:flex;gap:8px;align-items:center;flex-wrap:wrap;margin-bottom:12px}\n.rv-head h3{margin:0;font-family:'Barlow Condensed',sans-serif;font-size:24px;flex:1}\n.rv-fol{padding:6px 12px;border-radius:999px;font-size:14px}\n.rv-fol.on{background:transparent;border:1px solid rgba(255,255,255,.3);color:var(--text)}\n.rv-art h2{font-family:'Barlow Condensed',sans-serif;font-size:36px;line-height:1.05;margin:10px 0 6px}\n.rv-cov{width:100%;max-height:340px;object-fit:cover;border-radius:12px}\n.rv-by{color:var(--muted);margin:0 0 14px}\n.rv-by a{color:var(--text);font-weight:600}\n.rv-txt{font-size:17px;line-height:1.65;overflow-wrap:anywhere}\n.rv-txt blockquote{margin:12px 0;padding:4px 14px;border-left:3px solid var(--line);color:var(--muted)}\n.rv-txt h3,.rv-txt h4{font-family:'Barlow Condensed',sans-serif;font-size:24px;margin:18px 0 6px}\n.rv-txt a{color:var(--line);text-decoration:underline}\n.rv-h{font-family:'Barlow Condensed',sans-serif;font-size:20px;margin:18px 0 8px}\n.rv-tr{display:flex;align-items:center;gap:10px;padding:8px 10px;margin-bottom:6px;background:rgba(255,255,255,.04);border-radius:10px}\n.rv-tr>div{flex:1;min-width:0;display:flex;flex-direction:column}\n.rv-tr span,.rv-tr em{font-size:13px;color:var(--muted)}\n.rv-tr b{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}\n.rv-like{background:transparent;border:1px solid rgba(255,255,255,.2);color:var(--text);border-radius:999px;padding:8px 16px}\n.rv-like.on{border-color:var(--line);color:var(--line)}\n.rv-c{padding:8px 0;border-bottom:1px solid rgba(255,255,255,.06)}\n.rv-c p{margin:0 0 4px;overflow-wrap:anywhere}\n.rv-c small{color:var(--muted)}\n.rv-x{background:none;border:0;color:var(--muted);font-size:12px;padding:0 4px;text-decoration:underline}\n.rv-form label{display:block;margin:12px 0 4px}\n.rv-form input,.rv-form textarea,.rv-com textarea{width:100%}\n.rv-res{display:flex;width:100%;justify-content:flex-start;margin-bottom:4px;gap:8px}\n.rv-more{display:block;margin:0 auto 10px}\n/* ---- Estilos de art\u00edculo (solo valores permitidos por lista) ---- */\n.rv-st{--rb:rgba(255,255,255,.04);--rt:var(--text);--rm:var(--muted);--ra:var(--line);--rfh:'Barlow Condensed',sans-serif;--rfb:Barlow,system-ui,sans-serif;--rhs:1;--rbs:1;background:var(--rb);color:var(--rt)}\n.rv-st[data-th=senal]{--rb:#1b4f9c;--rt:#f4f6f8;--rm:#b9cbe6}\n.rv-st[data-th=papel]{--rb:#f1e9d8;--rt:#2a2620;--rm:#7a6f5c}\n.rv-st[data-th=neon]{--rb:#0b0a14;--rt:#f0eaff;--rm:#9a8fc2}\n.rv-st[data-th=atardecer]{--rb:linear-gradient(160deg,#3a1c3b,#b4452f 130%);--rt:#fff1e2;--rm:#f0c7a8}\n.rv-st[data-th=vinilo]{--rb:#0e0e0e;--rt:#eee;--rm:#999}\n.rv-st[data-ac=azul]{--ra:#4f8ff0}.rv-st[data-ac=coral]{--ra:#ff6b5a}.rv-st[data-ac=verde]{--ra:#3ecf8e}.rv-st[data-ac=violeta]{--ra:#a78bfa}.rv-st[data-ac=tinta]{--ra:var(--rt)}\n.rv-st[data-th=senal][data-ac=azul]{--ra:#ffffff}\n.rv-st[data-ft=editorial]{--rfh:'Playfair Display',Georgia,serif;--rfb:Lora,Georgia,serif}\n.rv-st[data-ft=maquina]{--rfh:'Space Mono',ui-monospace,monospace;--rfb:'Space Mono',ui-monospace,monospace;--rhs:.78;--rbs:.88}\n.rv-st[data-ft=manuscrita]{--rfh:Caveat,cursive;--rhs:1.3}\n.rv-st[data-ft=afiche]{--rfh:'Abril Fatface',Georgia,serif;--rhs:.92}\n.rv-st h2,.rv-st h3,.rv-st h4{font-family:var(--rfh)}\n.rv-st .rv-txt{font-family:var(--rfb);font-size:calc(17px*var(--rbs))}\n.rv-st .rv-sub,.rv-st .rv-m,.rv-st .rv-by,.rv-st .rv-tr span,.rv-st .rv-tr em,.rv-st .rv-txt blockquote{color:var(--rm)}\n.rv-st .rv-by a{color:var(--rt)}\n.rv-st .rv-txt a{color:var(--ra)}\n.rv-st .rv-txt blockquote{border-left-color:var(--ra)}\n.rv-st .rv-h{color:var(--ra)}\n.rv-st .alt{color:var(--rt);background:rgba(127,127,127,.14);border-color:rgba(127,127,127,.4)}\n.rv-st .rv-tr{background:rgba(127,127,127,.15)}\n.rv-st .rv-like{color:var(--rt);border-color:rgba(127,127,127,.5)}\n.rv-st .rv-like.on{color:var(--ra);border-color:var(--ra)}\n.rv-st .rv-fol:not(.on){background:var(--ra);color:#111;border-color:transparent}\n.rv-st .rv-fol.on{color:var(--rt)}\n.rv-art.rv-st{padding:16px;border-radius:14px;margin-bottom:14px}\n.rv-art.rv-st[data-th=asfalto]{background:transparent;padding:0}\n.rv-art.rv-st h2{font-size:calc(36px*var(--rhs));font-weight:700;color:var(--rt)}\n.rv-art.rv-st[data-sz=g] h2{font-size:calc(54px*var(--rhs))}\n.rv-card.rv-st h3{font-size:calc(22px*var(--rhs));color:var(--rt)}\n.rv-card.rv-st.hero h3{font-size:calc(32px*var(--rhs))}\n.rv-card.rv-st[data-sz=g] h3{font-size:calc(28px*var(--rhs))}\n.rv-card.rv-st[data-sz=g].hero h3{font-size:calc(42px*var(--rhs))}\n.rv-card.rv-st[data-th=asfalto]{border-color:rgba(255,255,255,.07)}\n.rv-card.rv-st:not([data-th=asfalto]){border-color:var(--ra)}\n.rv-card.rv-st[data-al=c] .rv-in,.rv-art[data-al=c] h2,.rv-art[data-al=c] .rv-sub,.rv-art[data-al=c] .rv-by{text-align:center}\n.rv-art[data-al=c] .rv-txt blockquote{text-align:center;border-left:0;border-top:2px solid var(--ra);border-bottom:2px solid var(--ra)}\n.rv-txt[data-dc=\"1\"]>p:first-of-type::first-letter{float:left;font-family:var(--rfh);font-size:3.6em;line-height:.82;padding:6px 10px 0 0;color:var(--ra)}\n.rv-hdr{position:relative;margin:-16px -16px 14px;border-radius:14px 14px 0 0;overflow:hidden}\n.rv-art.rv-st[data-th=asfalto] .rv-hdr{margin:0 0 14px;border-radius:14px}\n.rv-hdr .rv-cov{display:block;border-radius:0;max-height:420px;min-height:240px}\n.rv-ov{position:absolute;inset:auto 0 0 0;padding:60px 16px 14px;background:linear-gradient(transparent,rgba(0,0,0,.82));color:#fff}\n.rv-ov h2{color:#fff;margin:0 0 4px}\n.rv-ov .rv-sub{color:#e6e6e6;margin:0}\n.rv-sl{font-size:13px;color:var(--muted);margin:10px 0 4px;text-transform:uppercase;letter-spacing:.06em}\n.rv-sws{display:flex;gap:6px;flex-wrap:wrap}\n.rv-sw{display:inline-flex;align-items:center;gap:6px;background:rgba(255,255,255,.05);border:1px solid rgba(255,255,255,.14);color:var(--text);border-radius:999px;padding:6px 12px;font-size:14px}\n.rv-sw i{width:14px;height:14px;border-radius:50%;border:1px solid rgba(255,255,255,.4)}\n.rv-sw.on{border-color:var(--line);background:rgba(242,194,48,.14)}\n.rv-ft{font-size:16px}\n.rv-ft[data-ft=editorial]{font-family:'Playfair Display',serif}\n.rv-ft[data-ft=maquina]{font-family:'Space Mono',monospace;font-size:13px}\n.rv-ft[data-ft=manuscrita]{font-family:Caveat,cursive;font-size:20px}\n.rv-ft[data-ft=afiche]{font-family:'Abril Fatface',serif}\n.rv-ft[data-ft=cartel]{font-family:'Barlow Condensed',sans-serif;font-weight:700;font-size:17px}\n#rvPrev{margin-top:12px}\n/* ---- Revista v2: navegaci\u00f3n, perfiles, artistas, p\u00f3sters, est\u00e9ticas de revista ---- */\n.rv-bar{margin:4px 0 14px}\n.rv-secs{display:flex;gap:6px;align-items:center;margin-bottom:10px}\n.rv-sec{background:transparent;border:0;color:var(--muted);font-family:'Barlow Condensed',sans-serif;font-weight:700;font-size:20px;padding:4px 8px;border-bottom:3px solid transparent;border-radius:0}\n.rv-sec.on{color:var(--text);border-bottom-color:var(--line)}\n.rv-tabs{display:flex;gap:6px;flex-wrap:wrap;margin:0 0 12px}\n.rv-me{background:none;border:0;padding:0;border-radius:50%}\n.rv-av{border-radius:50%;object-fit:cover;flex:none;display:inline-flex;align-items:center;justify-content:center;vertical-align:middle;background:var(--ra,var(--line));color:#111;font-weight:700;font-family:'Barlow Condensed',sans-serif}\n.rv-fab{position:fixed;right:max(16px,calc(50vw - 244px));bottom:calc(96px + env(safe-area-inset-bottom,0px));z-index:40;width:58px;height:58px;border-radius:18px;font-size:32px;line-height:1;padding:0;display:flex;align-items:center;justify-content:center;box-shadow:0 10px 26px rgba(0,0,0,.45)}\n.rv-search{width:100%;margin:0 0 12px}\n.rv-stories{display:flex;gap:14px;overflow-x:auto;padding:4px 2px 12px;margin-bottom:6px;scrollbar-width:none}\n.rv-story{background:none;border:0;padding:0;display:flex;flex-direction:column;align-items:center;gap:6px;color:var(--text);width:70px;flex:none}\n.rv-story .rv-av{box-shadow:0 0 0 2px var(--asphalt),0 0 0 4px var(--line)}\n.rv-story span{font-size:12px;width:70px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;text-align:center}\n.rv-lab{margin:0;padding:6px 14px 0;font-size:12px;letter-spacing:.08em;text-transform:uppercase;color:var(--ra)}\n.rv-au{display:flex;align-items:center;gap:6px;font-size:14px;margin:2px 0 4px}\n.rv-m .ic{width:13px;height:13px;vertical-align:-2px}\n.rv-people{display:flex;gap:10px;overflow-x:auto;padding-bottom:10px;margin-bottom:10px}\n.rv-pc{flex:none;width:150px;background:rgba(255,255,255,.04);border:1px solid rgba(255,255,255,.08);border-radius:14px;padding:12px;display:flex;flex-direction:column;align-items:center;gap:6px;text-align:center}\n.rv-pc small{color:var(--muted);font-size:12px;min-height:30px}\n.rv-pcb{display:flex;gap:6px}.rv-pcb button{padding:6px 10px;font-size:13px}\n.rv-acts{display:flex;gap:8px;flex-wrap:wrap;margin:16px 0 4px}\n.rv-acts .ic,.rv-like .ic{width:16px;height:16px;vertical-align:-3px}\n.rv-by a{display:inline-flex;align-items:center;gap:6px;text-decoration:none}\n.rv-c a{color:var(--text);text-decoration:none;display:inline-flex;align-items:center;gap:6px}\n.rv-click{cursor:pointer}\n.rv-two{display:grid;grid-template-columns:1fr 1fr;gap:10px}\n.rv-two label{display:block;margin:12px 0 0}\n.rv-two input{width:100%;margin-top:4px}\n.rv-qrow{display:flex;gap:8px;margin-bottom:12px}.rv-qrow input{flex:1;min-width:0}\n/* Perfil */\n.rv-prof{padding:18px 16px;border-radius:16px;margin-bottom:14px}\n.rv-prof-top{display:flex;align-items:center;gap:14px}\n.rv-prof h2{margin:0;font-size:calc(30px*var(--rhs));line-height:1.05;color:var(--rt)}\n.rv-role{display:inline-block;margin-top:6px;font-size:12px;letter-spacing:.06em;text-transform:uppercase;padding:2px 8px;border-radius:999px;background:var(--ra);color:#111}\n.rv-bio{margin:12px 0 6px;font-family:var(--rfb);line-height:1.5}\n.rv-pbtns{display:flex;gap:8px;flex-wrap:wrap;margin-top:12px}\n.rv-pbtns .ic{width:15px;height:15px;vertical-align:-3px}\n/* Artistas: tarjetas grandes */\n.rv-agrid{display:grid;grid-template-columns:repeat(auto-fill,minmax(140px,1fr));gap:12px}\n.rv-acard{border-radius:16px;overflow:hidden;cursor:pointer;border:1px solid rgba(255,255,255,.08)}\n.rv-acard img,.rv-acard .rv-ph{width:100%;aspect-ratio:1;object-fit:cover;display:flex;align-items:center;justify-content:center}\n.rv-acard .rv-ph span{font-family:var(--rfh);font-size:64px;color:var(--ra)}\n.rv-acard h3{margin:0 0 2px;font-size:calc(22px*var(--rhs));line-height:1.05;color:var(--rt)}\n.rv-acard .rv-in{padding:10px 12px 12px}\n/* P\u00f3sters */\n.rv-pgrid{display:grid;grid-template-columns:repeat(auto-fill,minmax(140px,1fr));gap:12px;margin-bottom:14px}\n.rv-pcard{margin:0;cursor:pointer;background:rgba(255,255,255,.04);border:1px solid rgba(255,255,255,.08);border-radius:14px;overflow:hidden;display:flex;flex-direction:column}\n.rv-pcard img{width:100%;aspect-ratio:3/4;object-fit:cover;background:#111;display:block}\n.rv-pcard figcaption{padding:8px 10px 12px;display:flex;flex-direction:column;gap:3px}\n.rv-pcard b{font-size:15px;line-height:1.2;overflow:hidden;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical}\n.rv-pcard small{color:var(--muted);font-size:13px}\n.rv-pcard button{margin-top:6px;font-size:13px;padding:6px 8px}\n.rv-badge{align-self:flex-start;font-size:11px;font-weight:700;letter-spacing:.06em;text-transform:uppercase;padding:2px 8px;border-radius:999px}\n.rv-badge.og{background:var(--line);color:#111}\n.rv-badge.cm{background:rgba(79,143,240,.2);color:#9cc2ff;border:1px solid rgba(79,143,240,.5)}\n.rv-prow{display:flex;gap:10px;overflow-x:auto;padding-bottom:8px}\n.rv-pmini{margin:0;flex:none;width:120px;cursor:pointer}\n.rv-pmini img{width:120px;aspect-ratio:3/4;object-fit:cover;border-radius:10px;background:#111}\n.rv-pmini figcaption{font-size:12px;color:var(--muted);margin-top:4px}\n.rv-pview{margin:0}\n.rv-pview img{width:100%;max-height:78vh;object-fit:contain;border-radius:12px;background:#0d0f12}\n.rv-pdet{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin:12px 0 14px}\n.rv-pdet div{background:rgba(255,255,255,.05);border:1px solid rgba(255,255,255,.08);border-radius:12px;padding:8px 12px;min-width:0}\n.rv-pdet dt{font-size:11px;letter-spacing:.08em;text-transform:uppercase;color:var(--muted)}\n.rv-pdet dd{margin:2px 0 0;font-size:15px;overflow-wrap:anywhere}\nhtml[data-theme=light] .rv-pdet div{background:rgba(0,0,0,.04);border-color:rgba(0,0,0,.08)}\n.bv-blocked{display:flex;gap:14px;align-items:center;padding:14px;border-radius:18px;background:rgba(0,0,0,.35);border:1px solid rgba(255,255,255,.1)}\n.bv-blocked>img,.bv-blocked>.bv-noimg{width:42%;max-width:220px;aspect-ratio:16/9;object-fit:cover;border-radius:12px;flex:none;filter:grayscale(.4) brightness(.7)}\n.bv-blocked b{display:block;font-size:17px;line-height:1.3;margin-bottom:4px}\n.bv-blocked p{margin:0 0 10px;color:var(--muted)}\n.bv-blocked a.rv-like{display:inline-flex;text-decoration:none}\n@media (max-width:520px){.bv-blocked{flex-direction:column;align-items:stretch}.bv-blocked>img,.bv-blocked>.bv-noimg{width:100%;max-width:none}}\nhtml[data-theme=light] .bv-blocked{background:rgba(0,0,0,.05);border-color:rgba(0,0,0,.1)}\n#revBody{overflow-wrap:break-word}\n.rv-pview h2{font-family:'Barlow Condensed',sans-serif;font-size:30px;margin:10px 0 2px}\n.rv-pview a{color:var(--line)}\n/* ---- Est\u00e9ticas de revista ---- */\n.rv-st[data-ac=rojo]{--ra:#e3242b}.rv-st[data-ac=rosa]{--ra:#ff3d8b}\n.rv-st[data-lt=\"1\"][data-ac=amarillo]{--ra:#a87400}.rv-st[data-lt=\"1\"][data-ac=azul]{--ra:#1f56b8}.rv-st[data-lt=\"1\"][data-ac=coral]{--ra:#c4382a}.rv-st[data-lt=\"1\"][data-ac=verde]{--ra:#1f8a5b}.rv-st[data-lt=\"1\"][data-ac=violeta]{--ra:#6a49c8}.rv-st[data-lt=\"1\"][data-ac=rosa]{--ra:#d0216b}.rv-st[data-lt=\"1\"][data-ac=rojo]{--ra:#c01a20}\n.rv-st[data-th=y2k]{--rb:radial-gradient(rgba(255,100,40,.28) 1.3px,transparent 1.7px) 0 0/9px 9px,linear-gradient(135deg,#c4f24a,#3fdcf2 62%,#ff9a4d);--rt:#111;--rm:#2c2c2c;--rmc:#fff}\n.rv-st[data-th=punk]{--rb:url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='160' height='160'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.9' numOctaves='2'/%3E%3CfeColorMatrix values='0 0 0 0 1 0 0 0 0 1 0 0 0 0 1 0 0 0 .13 0'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E\"),#0b0b0b;--rt:#f1f1f1;--rm:#b8b8b8;--rmc:var(--ra)}\n.rv-st[data-th=clasica]{--rb:#f6f3ee;--rt:#161616;--rm:#5b5b5b;--rmc:var(--ra)}\n.rv-st[data-th=vintage]{--rb:url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='160' height='160'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.75' numOctaves='3'/%3E%3CfeColorMatrix values='0 0 0 0 .35 0 0 0 0 .25 0 0 0 0 .1 0 0 0 .16 0'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E\"),radial-gradient(ellipse at center,#f3ead4 55%,#dccaa2);--rt:#1a1714;--rm:#5e5345;--rmc:#1a1714}\n.rv-st[data-th=teen]{--rb:#fffaf2;--rt:#1c1c1c;--rm:#555;--rmc:var(--ra)}\n.rv-st[data-th=tokio]{--rb:repeating-linear-gradient(45deg,#fff0f6 0 12px,#ffe3ef 12px 24px);--rt:#2b1530;--rm:#6b4a6e;--rmc:#ffe14d}\n.rv-st[data-th=tabloide]{--rb:#fbfbf7;--rt:#111;--rm:#444;--rmc:#ffe600}\n.rv-st[data-th=cromo]{--rb:linear-gradient(160deg,#eef2f8,#b9c6d6 45%,#f4f7fb 55%,#a9bacd);--rt:#0f1d2e;--rm:#3b4f66;--rmc:#fff}\n.rv-st[data-ft=poster]{--rfh:Anton,'Barlow Condensed',sans-serif;--rhs:1.02}\n.rv-st[data-ft=retro]{--rfh:Shrikhand,Georgia,serif;--rhs:.9}\n.rv-st[data-ft=burbuja]{--rfh:'Rampart One','Barlow Condensed',sans-serif;--rhs:.92}\n.rv-st[data-ft=glitch]{--rfh:'Rubik Glitch','Barlow Condensed',sans-serif;--rhs:.88}\n.rv-st[data-ft=bloque]{--rfh:'Archivo Black','Barlow Condensed',sans-serif;--rhs:.86}\n.rv-ft[data-ft=poster]{font-family:Anton,sans-serif}.rv-ft[data-ft=retro]{font-family:Shrikhand,serif}.rv-ft[data-ft=burbuja]{font-family:'Rampart One',sans-serif}.rv-ft[data-ft=glitch]{font-family:'Rubik Glitch',sans-serif}.rv-ft[data-ft=bloque]{font-family:'Archivo Black',sans-serif}\n/* Titulares con efecto seg\u00fan la est\u00e9tica */\n.rv-st[data-th=y2k] :is(h2,h3){color:#fff;-webkit-text-stroke:1.5px #111;paint-order:stroke fill;text-shadow:3px 3px 0 var(--ra)}\n.rv-st[data-th=punk] :is(h2,h3){color:var(--ra);text-transform:lowercase;letter-spacing:-.01em}\n.rv-st[data-th=clasica] :is(h2,h3){color:var(--ra);text-shadow:2px 2px 0 #111}\n.rv-st[data-th=vintage] :is(h2,h3){text-transform:uppercase;letter-spacing:.01em;color:#1a1714}\n.rv-st[data-th=teen] :is(h2,h3){text-transform:lowercase;letter-spacing:-.02em;color:#1c1c1c}\n.rv-st[data-th=teen] .rv-sub{color:var(--ra)}\n.rv-st[data-th=tokio] :is(h2,h3){color:#ffe14d;-webkit-text-stroke:2px #ff3d8b;paint-order:stroke fill;text-shadow:3px 3px 0 #7a2bff}\n.rv-st[data-th=tabloide] :is(h2,h3){color:#ffe600;-webkit-text-stroke:2px #d0021b;paint-order:stroke fill;text-shadow:3px 3px 0 #111}\n.rv-st[data-th=cromo] :is(h2,h3){background:linear-gradient(180deg,#fff 0%,#9fb3c8 46%,#2f4359 52%,#dfe8f2 100%);-webkit-background-clip:text;background-clip:text;color:transparent;filter:drop-shadow(0 1px 0 rgba(0,0,0,.35))}\n.rv-st[data-th=teen] .rv-txt blockquote{background:#3d5afe;color:#fff;border:0;padding:10px 14px;transform:rotate(-1deg)}\n.rv-st[data-th=tabloide] .rv-txt blockquote{background:repeating-linear-gradient(0deg,#1d4ed8 0 4px,#2b5ff0 4px 8px);color:#fff;border:0;padding:10px 14px}\n.rv-st[data-th=punk] .rv-txt blockquote{background:var(--ra);color:#111;border:0;padding:8px 12px;transform:rotate(1deg)}\n.rv-art.rv-st[data-th=tabloide]{border-top:14px solid;border-image:repeating-linear-gradient(0deg,#1d4ed8 0 3px,#fff 3px 6px) 14}\n/* Portada de revista */\n.rv-mag{position:relative;aspect-ratio:3/4;max-height:640px;margin:-16px -16px 14px;overflow:hidden;border-radius:14px 14px 0 0;background:var(--rb);isolation:isolate}\n.rv-art.rv-st[data-th=asfalto] .rv-mag{margin:0 0 14px;border-radius:14px;background:#262a31}\n.rv-mag-img{position:absolute;inset:0;width:100%;height:100%;object-fit:cover;z-index:-1}\n.rv-mag::after{content:'';position:absolute;inset:0;z-index:-1;background:linear-gradient(180deg,rgba(0,0,0,.18) 0%,transparent 28%,transparent 55%,rgba(0,0,0,.6) 100%);pointer-events:none}\n.rv-st[data-lt=\"1\"] .rv-mag:not(:has(.rv-mag-img))::after{background:none}\n.rv-mag-strip{position:absolute;top:0;left:0;right:0;background:var(--ra);color:#111;font-size:11px;font-weight:700;letter-spacing:.12em;text-transform:uppercase;white-space:nowrap;overflow:hidden;padding:3px 8px}\n.rv-mag-mast{position:absolute;top:22px;left:12px;right:12px;font-family:var(--rfh);font-size:calc(52px*var(--rhs));line-height:.95;color:var(--rmc,var(--ra));text-transform:uppercase;letter-spacing:-.01em;text-shadow:2px 2px 0 rgba(0,0,0,.35);overflow-wrap:anywhere}\n.rv-st[data-th=tokio] .rv-mag-mast,.rv-st[data-th=tabloide] .rv-mag-mast,.rv-st[data-th=y2k] .rv-mag-mast{-webkit-text-stroke:2px #d0021b;paint-order:stroke fill;text-shadow:4px 4px 0 #111}\n.rv-st[data-th=tokio] .rv-mag-mast{-webkit-text-stroke-color:#ff3d8b;text-shadow:4px 4px 0 #7a2bff}\n.rv-st[data-th=y2k] .rv-mag-mast{-webkit-text-stroke-color:#111;text-shadow:4px 4px 0 var(--ra)}\n.rv-st[data-th=cromo] .rv-mag-mast{background:linear-gradient(180deg,#fff,#9fb3c8 46%,#2f4359 52%,#dfe8f2);-webkit-background-clip:text;background-clip:text;color:transparent;text-shadow:none;filter:drop-shadow(0 2px 0 rgba(0,0,0,.4))}\n.rv-st[data-th=clasica] .rv-mag-mast{text-transform:none;font-style:italic;text-shadow:3px 3px 0 #111}\n.rv-st[data-th=teen] .rv-mag-mast,.rv-st[data-th=punk] .rv-mag-mast{text-transform:lowercase}\n.rv-st[data-al=c] .rv-mag-mast,.rv-st[data-al=c] .rv-mag-lines{text-align:center}\n.rv-mag-lines{position:absolute;left:14px;right:96px;bottom:16px}\n.rv-mag-lines h2{margin:0 0 4px!important;font-size:calc(40px*var(--rhs))!important;line-height:1!important;color:#fff;text-shadow:0 2px 12px rgba(0,0,0,.45)}\n.rv-art.rv-st[data-sz=g] .rv-mag-lines h2{font-size:calc(58px*var(--rhs))!important}\n.rv-mag-lines .rv-sub{color:#fff!important;font-weight:600;text-shadow:0 1px 6px rgba(0,0,0,.6);margin:0}\n.rv-st[data-lt=\"1\"] .rv-mag:not(:has(.rv-mag-img)) .rv-mag-lines :is(h2,.rv-sub){text-shadow:none}\n.rv-st[data-lt=\"1\"] .rv-mag:not(:has(.rv-mag-img)) .rv-mag-lines .rv-sub{color:var(--rt)!important}\n.rv-stk{position:absolute;top:30%;right:10px;width:96px;height:96px;display:flex;align-items:center;justify-content:center;transform:rotate(12deg);background:var(--ra);clip-path:polygon(50% 0,61% 18%,82% 9%,79% 31%,100% 38%,84% 54%,96% 74%,74% 76%,68% 98%,50% 84%,32% 98%,26% 76%,4% 74%,16% 54%,0 38%,21% 31%,18% 9%,39% 18%)}\n.rv-stk span{color:#111;font-weight:800;font-size:13px;line-height:1.05;text-align:center;text-transform:uppercase;padding:0 18px;font-family:var(--rfh);overflow-wrap:anywhere}\n.rv-code{position:absolute;right:12px;bottom:14px;width:62px;height:40px;background:#fff linear-gradient(90deg,#111 0 2px,transparent 2px 4px,#111 4px 5px,transparent 5px 8px,#111 8px 11px,transparent 11px 12px,#111 12px 13px,transparent 13px 16px,#111 16px 18px,transparent 18px 19px,#111 19px 22px,transparent 22px 24px,#111 24px 25px,transparent 25px 28px,#111 28px 29px,transparent 29px 31px) 4px 4px/54px 26px no-repeat;border-radius:2px;box-shadow:0 2px 6px rgba(0,0,0,.3)}\n.rv-mag.sm{margin:0;border-radius:0;max-height:none}\n.rv-mag.sm .rv-mag-mast{font-size:calc(30px*var(--rhs));top:18px}\n.rv-mag.sm .rv-mag-lines{right:70px;bottom:10px;left:10px}\n.rv-mag.sm .rv-mag-lines h2{font-size:calc(22px*var(--rhs))!important}\n.rv-card.hero .rv-mag.sm .rv-mag-mast{font-size:calc(48px*var(--rhs))}\n.rv-card.hero .rv-mag.sm .rv-mag-lines h2{font-size:calc(34px*var(--rhs))!important}\n.rv-mag.sm .rv-stk{width:64px;height:64px;right:6px}.rv-mag.sm .rv-stk span{font-size:9px;padding:0 12px}\n.rv-mag.sm .rv-code{width:40px;height:26px;right:8px;bottom:10px;background-size:32px 16px}\n.rv-mag.sm .rv-mag-strip{font-size:9px;padding:2px 6px}\n.rv-card.mag .rv-in{padding:8px 12px 12px}\n#rvPrev .rv-mag{max-height:420px}\n@media (prefers-reduced-motion:no-preference){.rv-mag-strip span{display:inline-block;animation:rvTicker 18s linear infinite}@keyframes rvTicker{from{transform:translateX(0)}to{transform:translateX(-50%)}}}\n/* ---- Paquete 1: protecci\u00f3n y panel de admin ---- */\n.rv-warn{margin:0 0 10px;padding:8px 12px;border-radius:10px;background:rgba(229,96,77,.15);border:1px solid rgba(229,96,77,.5);color:#ffb3a7;font-size:14px}\n.rv-rep{background:rgba(255,255,255,.04);border:1px solid rgba(255,255,255,.08);border-left:3px solid var(--alert);border-radius:12px;padding:10px 12px;margin-bottom:10px}\n.rv-rep p{margin:0 0 4px}.rv-rep-t{font-weight:600;overflow-wrap:anywhere}\n.rv-rep ul{margin:6px 0 10px;padding-left:18px;font-size:14px}.rv-rep li small{color:var(--muted)}\n.rv-urow{display:flex;align-items:center;gap:10px;padding:10px 0;border-bottom:1px solid rgba(255,255,255,.06)}\n.rv-urow>div:first-of-type,.rv-urow>div:only-child{flex:1;min-width:0;display:flex;flex-direction:column}\n.rv-urow small{color:var(--muted);font-size:13px}.rv-bad{color:var(--alert)}\n.rv-kpis{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin-bottom:10px}\n.rv-kpi{background:rgba(255,255,255,.05);border-radius:12px;padding:10px;text-align:center;display:flex;flex-direction:column}\n.rv-kpi b{font-family:'Barlow Condensed',sans-serif;font-size:30px;line-height:1;color:var(--line)}.rv-kpi span{font-size:12px;color:var(--muted)}\n.rv-acts .rv-x,.rv-pbtns .rv-x{align-self:center}\n.rv-secs{overflow-x:auto;scrollbar-width:none;gap:2px}\n.rv-secs::-webkit-scrollbar{display:none}\n.rv-sec{flex:none;font-size:17px;padding:4px 5px}\n.rv-secs .rv-me,.rv-secs>.alt{flex:none}\n.rv-form label.chk{display:flex;gap:10px;align-items:flex-start}\n.rv-form .chk input{width:auto;flex:none;margin-top:4px}\n/* ---- Paquetes 2 y 3 ---- */\n.rv-bell{position:relative;background:none;border:0;padding:6px;color:var(--text);min-height:0}\n.rv-bell .ic{width:22px;height:22px}\n.rv-bell b{position:absolute;top:0;right:0;min-width:17px;height:17px;border-radius:9px;background:var(--alert);color:#fff;font-size:11px;line-height:17px;text-align:center;padding:0 4px}\n.rv-new{background:rgba(242,194,48,.07)}\n.rv-urow>div>span{line-height:1.35}\n.rv-rules>div{display:flex;gap:12px;align-items:flex-start;margin-bottom:12px}\n.rv-rules span{flex:none;width:28px;height:28px;border-radius:50%;background:var(--line);color:#111;font-weight:700;display:flex;align-items:center;justify-content:center}\n.rv-rules p{margin:2px 0 0;line-height:1.45}\n.rv-reply{margin-left:22px;padding-left:12px;border-left:2px solid rgba(255,255,255,.12)}\n.rv-replyto{margin:10px 0 6px;font-size:14px;color:var(--muted)}\n.rv-like.on{color:var(--ra,var(--line));border-color:var(--ra,var(--line))}\n.rv-listen{margin:0 0 12px}\n.rv-badges{display:flex;flex-wrap:wrap;gap:6px;margin:10px 0 2px}\n.rv-badge2{display:inline-flex;align-items:center;gap:5px;font-size:12px;font-weight:600;padding:3px 10px 3px 4px;border-radius:999px;background:rgba(127,127,127,.16);border:1px solid rgba(127,127,127,.35);color:var(--rt,var(--text))}\n.rv-badge2 i{font-style:normal;width:20px;height:20px;border-radius:50%;background:var(--ra,var(--line));color:#111;display:flex;align-items:center;justify-content:center;font-size:11px}\n.rv-poll{background:rgba(255,255,255,.04);border:1px solid rgba(255,255,255,.08);border-radius:14px;padding:12px 14px;margin-bottom:12px}\n.rv-poll h3{font-family:'Barlow Condensed',sans-serif;font-size:24px;margin:4px 0 10px;line-height:1.1}\n.rv-poll .rv-au a{color:var(--text);text-decoration:none}\n.rv-opt{position:relative;display:flex;justify-content:space-between;align-items:center;width:100%;margin-bottom:6px;background:rgba(255,255,255,.05);border:1px solid rgba(255,255,255,.14);color:var(--text);border-radius:10px;padding:10px 12px;overflow:hidden;text-align:left}\n.rv-opt span{position:absolute;inset:0 auto 0 0;background:rgba(242,194,48,.22);z-index:0;transition:width .4s}\n.rv-opt b,.rv-opt em{position:relative;z-index:1;font-style:normal}\n.rv-opt.on{border-color:var(--line)}.rv-opt.on b::after{content:' \u2713';color:var(--line)}\n.rv-chal{border-radius:16px;padding:16px;margin-bottom:12px}\n.rv-chal h2{margin:4px 0 6px;font-size:calc(30px*var(--rhs));line-height:1.05}\n.rv-chal .rv-mag{display:none}\n.rv-review{display:flex;gap:12px;align-items:center;background:rgba(127,127,127,.12);border-radius:12px;padding:10px 12px;margin:0 0 14px}\n.rv-review>div{flex:1;display:flex;flex-direction:column;gap:2px}\n.rv-review small{color:var(--rm,var(--muted))}\n.rv-review i{display:block;height:6px;border-radius:3px;background:linear-gradient(90deg,var(--ra,var(--line)) var(--s),rgba(127,127,127,.3) var(--s));margin-top:4px}\n.rv-score{flex:none;width:56px;height:56px;border-radius:50%;background:var(--ra,var(--line));color:#111;font-family:'Barlow Condensed',sans-serif;font-weight:700;font-size:28px;display:flex;align-items:center;justify-content:center}\n.rv-score small{font-size:12px}.rv-score.sm{width:36px;height:36px;font-size:18px}\n.rv-scorechip{display:inline-block;font-size:12px;font-weight:700;padding:2px 8px;border-radius:999px;background:var(--ra);color:#111;margin-bottom:6px;max-width:100%;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}\n.rv-avg{font-size:15px;margin:0 0 6px}.rv-avg b{font-family:'Barlow Condensed',sans-serif;font-size:30px;color:var(--line)}\n.rv-disc{margin:0;flex:none;width:120px}\n.rv-disc img,.rv-disc .rv-ph{width:120px;height:120px;border-radius:8px;object-fit:cover;background:#111;display:flex;align-items:center;justify-content:center;color:var(--muted);font-size:30px}\n.rv-disc figcaption{display:flex;flex-direction:column;font-size:12px;margin-top:4px}.rv-disc small{color:var(--muted)}\n.rv-ok{display:inline-flex;align-items:center;justify-content:center;font-size:12px;font-weight:700;padding:1px 7px;border-radius:999px;background:#3ecf8e;color:#0b2a1c;vertical-align:middle}\n.rv-verif{margin:0 0 10px;font-size:14px;color:var(--rm,var(--muted))}\n.rv-links{display:flex;flex-wrap:wrap;gap:8px;margin:0 0 12px}\n.rv-links a{font-size:14px;padding:5px 12px;border-radius:999px;border:1px solid rgba(127,127,127,.5);color:var(--rt,var(--text));text-decoration:none}\n.rv-adminbox{border:1px dashed rgba(242,194,48,.5);border-radius:12px;padding:10px 12px;margin:12px 0}\n.rv-today{background:linear-gradient(135deg,rgba(242,194,48,.12),rgba(27,79,156,.18));border:1px solid rgba(242,194,48,.3);border-radius:14px;padding:10px 14px;margin-bottom:14px}\n.rv-today .rv-lab{padding:0;margin-bottom:6px;color:var(--line)}\n.rv-today>div{display:flex;gap:10px;margin-bottom:8px}\n.rv-today>div>b{flex:none;font-family:'Barlow Condensed',sans-serif;font-size:22px;color:var(--line);min-width:46px}\n.rv-today p{margin:0;font-size:14px;line-height:1.4}.rv-today a{color:var(--line)}\n.rv-ev{display:flex;gap:12px;align-items:flex-start;padding:10px 0;border-bottom:1px solid rgba(255,255,255,.06)}\n.rv-evd{flex:none;width:54px;text-align:center;background:rgba(242,194,48,.12);border-radius:10px;padding:6px 0;display:flex;flex-direction:column}\n.rv-evd b{font-family:'Barlow Condensed',sans-serif;font-size:28px;line-height:1;color:var(--line)}.rv-evd span,.rv-evd small{font-size:11px;color:var(--muted);text-transform:uppercase}\n.rv-evi{flex:1;min-width:0;display:flex;flex-direction:column;gap:3px}.rv-evi small{color:var(--muted)}\n.rv-evi .rv-pcb{margin-top:6px;flex-wrap:wrap}.rv-evi .rv-like{padding:4px 10px;font-size:13px;text-decoration:none}\n.rv-ev>img{flex:none;width:64px;aspect-ratio:3/4;object-fit:cover;border-radius:8px}\n.rv-map{height:60vh;min-height:320px;border-radius:14px;overflow:hidden;background:#1b1e23}\n.rv-web{display:flex;flex-direction:column;gap:8px;margin-bottom:8px}\n.rv-wcard{display:grid;grid-template-columns:52px 1fr;grid-template-rows:auto;column-gap:10px;row-gap:2px;padding:10px 12px;border-radius:12px;background:rgba(255,255,255,.04);border:1px solid rgba(255,255,255,.1);color:var(--text);text-decoration:none}\n.rv-wcard>*{grid-column:2}.rv-wyear{grid-column:1!important;grid-row:1/span 3;font-family:'Barlow Condensed',sans-serif;font-size:22px;color:var(--line);align-self:center}\n.rv-wcard small{color:var(--muted);font-size:13px}.rv-wcard i{font-style:normal;color:#3ecf8e}.rv-src{color:var(--line)!important}\n.rv-srcs a{color:var(--muted)}\n.rv-top{display:flex;align-items:center;justify-content:space-between;gap:10px}\n.rv-top .ptitle{margin-right:auto}\n.rv-meb{display:inline-flex;align-items:center;gap:6px}\n.rv-evi .rv-ok{align-self:flex-start}\n.rv-sec{font-size:17px;padding:4px 6px}\n/* ---- Paquetes 4 y 5 ---- */\n.rv-tags{display:flex;flex-wrap:wrap;gap:4px 8px;margin:2px 0 6px}\n.rv-tag{font-size:12px;color:var(--ra,var(--line));text-decoration:none;font-weight:600}\n.rv-stt{display:inline-block;font-size:11px;font-weight:700;letter-spacing:.04em;text-transform:uppercase;padding:2px 8px;border-radius:999px;background:rgba(127,127,127,.25);color:var(--rt,var(--text));margin:0 6px 6px 0}\n.rv-tagf{margin:0 0 12px;font-size:15px}\n.rv-yt{position:relative;display:block;width:100%;aspect-ratio:16/9;padding:0;border:0;border-radius:12px;overflow:hidden;background:#000;margin:12px 0;cursor:pointer}\n.rv-yt img{width:100%;height:100%;object-fit:cover;opacity:.85}\n.rv-yt span{position:absolute;inset:0;margin:auto;width:68px;height:48px;border-radius:12px;background:rgba(220,0,0,.9);color:#fff;font-size:22px;display:flex;align-items:center;justify-content:center}\n.rv-ytf{position:relative;aspect-ratio:16/9;margin:12px 0;border-radius:12px;overflow:hidden}.rv-ytf iframe{position:absolute;inset:0;width:100%;height:100%;border:0}\n.rv-tl{list-style:none;margin:14px 0;padding:0 0 0 14px;border-left:3px solid var(--ra,var(--line))}\n.rv-tl li{position:relative;margin:0 0 12px;padding-left:10px;display:flex;flex-direction:column}\n.rv-tl li::before{content:'';position:absolute;left:-22px;top:5px;width:13px;height:13px;border-radius:50%;background:var(--ra,var(--line))}\n.rv-tl b{font-family:var(--rfh,'Barlow Condensed'),sans-serif;font-size:20px;color:var(--ra,var(--line));line-height:1.1}\n.rv-gal{display:flex;gap:8px;overflow-x:auto;padding:4px 0 10px;scroll-snap-type:x mandatory}\n.rv-galb{flex:none;width:72%;max-width:320px;padding:0;border:0;background:none;scroll-snap-align:start;position:relative}\n.rv-galb img{width:100%;aspect-ratio:4/3;object-fit:cover;border-radius:10px;display:block}\n.rv-galb .rv-x{position:absolute;right:6px;bottom:6px;background:rgba(0,0,0,.6);border-radius:6px;padding:2px 6px;color:#fff}\n#rvGalPrev .rv-galb{width:120px}\n.rv-lb{position:fixed;inset:0;z-index:300;background:rgba(0,0,0,.92);display:flex;align-items:center;justify-content:center}\n.rv-lb img{max-width:96vw;max-height:92vh;object-fit:contain}.rv-lb button{position:absolute;top:12px;right:12px;background:rgba(255,255,255,.15);color:#fff;border:0;border-radius:50%;width:40px;height:40px;font-size:18px}\n.rv-aiw{display:flex;flex-wrap:wrap;gap:4px 10px;align-items:center;font-size:13px;color:var(--muted);margin:6px 0 2px}\n.rv-pick{display:flex;gap:10px;align-items:flex-start;padding:6px 0;border-bottom:1px solid rgba(255,255,255,.06)}\n.rv-pick span{display:flex;flex-direction:column}.rv-pick small{color:var(--muted)}\n.rv-cmp{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-bottom:10px}\n.rv-cmp img{width:100%;aspect-ratio:3/4;object-fit:contain;background:#0d0f12;border-radius:10px}.rv-cmp p{font-size:13px;margin:6px 0 0}\n\n/* La b\u00f3veda del tiempo */\n.bv-mast{position:relative;margin:4px 0 14px;padding:18px 16px 16px;border-radius:14px;background:radial-gradient(120% 140% at 0% 0%,rgba(242,194,48,.16),transparent 55%),linear-gradient(160deg,#0d0f12,#16191e 60%,#1d1a12);border:1px solid rgba(242,194,48,.22);overflow:hidden}\n.bv-mast::after{content:\"\";position:absolute;inset:0;background:repeating-linear-gradient(90deg,transparent 0 22px,rgba(255,255,255,.025) 22px 24px);pointer-events:none}\n.bv-kick{margin:0 0 4px;font-size:11px;letter-spacing:.14em;text-transform:uppercase;color:var(--line)}\n.bv-mast h2{margin:0;font-family:'Barlow Condensed',sans-serif;font-weight:700;font-size:40px;line-height:.95;text-transform:uppercase;letter-spacing:.01em}\n.bv-sub{margin:8px 0 0;color:var(--muted);font-size:14px;max-width:46ch}\n.bv-tabs{margin-bottom:14px}\n.bv-hero{display:block;width:100%;padding:0;margin:0 0 16px;background:none;border:0;text-align:left;color:var(--text);border-radius:14px;cursor:pointer}\n.bv-frame{position:relative;display:block;aspect-ratio:16/9;border-radius:12px;overflow:hidden;background:#0b0c0e;box-shadow:0 8px 24px rgba(0,0,0,.45)}\n.bv-frame img{width:100%;height:100%;object-fit:cover;display:block;transition:transform .4s}\n.bv-hero:hover .bv-frame img,.bv-card:hover .bv-frame img{transform:scale(1.03)}\n.bv-hero .bv-frame{aspect-ratio:1/1}\n.bv-shade{position:absolute;inset:0;background:linear-gradient(to top,rgba(0,0,0,.94) 0%,rgba(0,0,0,.6) 40%,rgba(0,0,0,.1) 70%,transparent 85%)}\n.bv-hcap{position:absolute;left:0;right:0;bottom:0;padding:14px;display:flex;flex-direction:column;gap:3px}\n.bv-hcap b{font-family:'Barlow Condensed',sans-serif;font-size:28px;line-height:1.02;text-transform:uppercase}\n.bv-hcap small{color:#d9dde3;font-size:13px}\n.bv-lab{font-size:11px;letter-spacing:.12em;text-transform:uppercase;color:var(--line);font-weight:700;margin:0}\n.bv-go{margin-top:6px;display:inline-flex;gap:10px;align-items:center;align-self:flex-start;background:var(--line);color:#1b1e23;font-weight:700;border-radius:999px;padding:6px 14px;font-size:14px}\n.bv-go i{font-style:normal;font-weight:500;opacity:.8}\n.bv-quote{display:block;margin:10px 2px 0;color:var(--muted);font-style:italic;font-size:14px;line-height:1.45}\n.bv-cycles{display:flex;gap:6px;flex-wrap:wrap;align-items:center;margin:4px 0 6px}\n.bv-cycles .rv-tab{padding:5px 12px;font-size:13px}\n.bv-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(160px,1fr));gap:14px}\n.bv-card{display:flex;flex-direction:column;gap:4px;padding:0;background:none;border:0;text-align:left;color:var(--text);cursor:pointer;min-height:0;font-weight:400}\n.bv-card b{font-size:14px;line-height:1.25;margin-top:4px}\n.bv-card small{color:var(--muted);font-size:12px;line-height:1.3}\n.bv-card .bv-frame{box-shadow:0 4px 14px rgba(0,0,0,.4)}\n.bv-hid{opacity:.55}\n.bv-warn{color:#f0a35e!important}\n.bv-rib{position:absolute;left:8px;top:8px;background:rgba(10,11,13,.82);color:#fff;font-size:11px;font-weight:700;letter-spacing:.03em;padding:3px 8px;border-radius:4px;backdrop-filter:blur(4px)}\n.bv-ribn{background:var(--line);color:#1b1e23}\n.bv-ribw{background:#c0392b}\n.bv-rib0{background:rgba(60,64,70,.9)}\n.bv-noimg{position:absolute;inset:0;display:flex;align-items:center;justify-content:center;font-family:'Barlow Condensed',sans-serif;font-size:56px;color:rgba(242,194,48,.5);background:linear-gradient(135deg,#191c21,#0c0d10)}\n.bv-gone .bv-frame::after,.bv-gone.bv-play::after{content:\"No disponible\";position:absolute;inset:0;display:flex;align-items:center;justify-content:center;background:rgba(0,0,0,.7);color:#fff;font-weight:700;font-size:13px;pointer-events:none}\n.bv-empty{padding:28px 18px;border:1px dashed rgba(242,194,48,.35);border-radius:12px;text-align:center}\n.bv-empty b{font-family:'Barlow Condensed',sans-serif;font-size:24px}.bv-empty p{color:var(--muted);margin:6px 0 0}\n.bv-curs{margin:20px 0 0;display:flex;gap:10px;flex-wrap:wrap;align-items:center;font-size:13px;color:var(--muted)}\n.bv-curs a{display:inline-flex;gap:6px;align-items:center;color:var(--text);text-decoration:none}\n.bv-legal{margin:16px 0 0;font-size:12px;color:var(--muted);line-height:1.45;border-top:1px solid rgba(255,255,255,.08);padding-top:10px}\n.bv-item{max-width:820px}\n.bv-play{position:relative;border-radius:12px;overflow:hidden;margin:0 0 10px;background:#000}\n.bv-poster{position:relative;display:block;width:100%;aspect-ratio:16/9;padding:0;border:0;border-radius:0;background:#000;cursor:pointer}\n.bv-poster img{width:100%;height:100%;object-fit:cover;opacity:.85;display:block}\n.bv-pbtn{position:absolute;inset:0;margin:auto;width:76px;height:76px;border-radius:50%;background:rgba(242,194,48,.95);color:#1b1e23;font-size:30px;display:flex;align-items:center;justify-content:center;box-shadow:0 6px 24px rgba(0,0,0,.5);padding-left:5px}\n.bv-ytf{margin:0;border-radius:0}\n.bv-closed{aspect-ratio:16/9}\n.bv-closed img{width:100%;height:100%;object-fit:cover;filter:grayscale(1) brightness(.45);display:block}\n.bv-closed>span:last-child{position:absolute;inset:0;display:flex;align-items:center;justify-content:center;color:#fff;font-family:'Barlow Condensed',sans-serif;font-size:22px;text-transform:uppercase;letter-spacing:.04em}\n.bv-status{margin:4px 0 2px;font-size:13px;color:var(--muted)}\n.bv-on{color:#3ecf8e;font-weight:700}.bv-soon{color:var(--line)}\n.bv-t{margin:4px 0 2px;font-family:'Barlow Condensed',sans-serif;font-size:34px;line-height:1;text-transform:uppercase}\n.bv-meta{margin:0 0 8px;color:var(--muted);font-size:15px;display:flex;gap:8px;flex-wrap:wrap;align-items:center}\n.bv-meta .rv-tag{background:none;border:1px solid rgba(242,194,48,.4);color:var(--line);border-radius:999px;padding:2px 10px;min-height:0;font-size:12px}\n.bv-src{font-size:13px;color:var(--muted);margin:0 0 10px;line-height:1.5}\n.bv-src a{color:var(--line)}\n.bv-tools{display:flex;gap:6px;flex-wrap:wrap;align-items:center;margin:10px 0;padding:10px;border-radius:10px;background:rgba(242,194,48,.07);border:1px solid rgba(242,194,48,.2)}\n.bv-tools button{padding:6px 10px;font-size:13px;min-height:34px}\n.bv-note{margin:16px 0;padding:14px 16px;border-left:3px solid var(--line);background:rgba(255,255,255,.03);border-radius:0 10px 10px 0}\n.bv-note .rv-txt{margin-top:6px}.bv-note a{color:var(--line)}\n.bv-set{margin:0;padding-left:0;list-style:none;counter-reset:bv}\n.bv-set li{counter-increment:bv;display:flex;gap:8px;padding:6px 0;border-bottom:1px solid rgba(255,255,255,.06);font-size:14px}\n.bv-set li::before{content:counter(bv);color:var(--muted);min-width:20px;text-align:right;font-variant-numeric:tabular-nums}\n.bv-set i{font-style:normal;color:var(--line);font-variant-numeric:tabular-nums;margin-right:4px}\n.bv-seek{background:none;border:0;padding:0;min-height:0;color:var(--text);text-align:left;font-weight:400;cursor:pointer}\n.bv-seek:hover{color:var(--line)}\n.bv-rule{font-size:13px;line-height:1.45;padding:10px 12px;border-radius:10px;background:rgba(62,207,142,.08);border:1px solid rgba(62,207,142,.25);margin:0 0 6px}\n.bv-row{display:flex;gap:10px;align-items:flex-end}.bv-row>*{flex:1;min-width:0}.bv-row>button{flex:none}\n.bv-good{color:#3ecf8e}.bv-bad{color:#ff7b6b}\n.bv-cov img{max-width:220px;border-radius:8px;display:block;margin-bottom:6px}\n@media (min-width:600px){.bv-hero .bv-frame{aspect-ratio:16/9}}\n@media (min-width:900px){.bv-hero .bv-frame{aspect-ratio:21/9}.bv-mast h2{font-size:54px}.bv-hcap b{font-size:38px}.bv-grid{grid-template-columns:repeat(auto-fill,minmax(210px,1fr))}}\n.bv-syn{display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;color:var(--muted);opacity:.85}\n.bv-hsyn{font-style:normal;color:var(--text);opacity:.85}\n.bv-synb{margin:4px 0 12px}.bv-synb p:last-child{margin:4px 0 0;font-size:16px;line-height:1.55}\np.bv-warn{color:#f0a35e;font-size:13px;margin:4px 0 10px}\n.bv-log{display:flex;flex-direction:column;gap:2px;margin:8px 0}\n.bv-lrow{padding:8px 0;border-bottom:1px solid rgba(255,255,255,.07)}.bv-lrow p{margin:0;font-size:14px}.bv-lrow .rv-m{font-size:12px;margin-top:2px}\n.bv-diff{margin:6px 0 2px;padding:8px 10px;border-radius:8px;background:rgba(255,255,255,.04);font-size:13px}.bv-diff p{margin:2px 0}.bv-diff s{opacity:.6}\n.bv-atabs{margin:4px 0 12px}\n.bv-perms{display:flex;gap:12px;flex-wrap:wrap;margin-top:4px}.bv-perms .chk{font-size:13px;margin:0}\n.bv-arow{display:grid;grid-template-columns:96px 1fr;gap:10px;padding:10px 0;border-bottom:1px solid rgba(255,255,255,.07);align-items:start}\n.bv-arow .rv-pcb{grid-column:1/-1;margin:0}\n.bv-athumb{padding:0;border:0;background:#000;border-radius:8px;overflow:hidden;aspect-ratio:16/9;position:relative;min-height:0}\n.bv-athumb img{width:100%;height:100%;object-fit:cover;display:block}\n.bv-ainfo{display:flex;flex-direction:column;gap:3px;min-width:0}.bv-ainfo b{font-size:15px}.bv-ainfo small{font-size:12px;color:var(--muted)}\n.bv-st{display:inline-block;font-weight:700;font-size:11px;padding:1px 7px;border-radius:999px;background:rgba(255,255,255,.08);color:var(--text)}\n.bv-st-now{background:rgba(62,207,142,.18);color:#3ecf8e}.bv-st-soon{background:rgba(242,194,48,.18);color:var(--line)}.bv-st-hidden{background:rgba(240,163,94,.18);color:#f0a35e}\n@media (min-width:700px){.bv-arow{grid-template-columns:120px 1fr auto}.bv-arow .rv-pcb{grid-column:auto}}\n\n/* ===== Revista viva: aire moderno, c\u00e1lido y juvenil ===== */\n#view-revista{--nv-s1:#23252c;--nv-s2:#2b2e36;--nv-bd:rgba(255,255,255,.07);--nv-warm:#ff8a5c;--nv-pink:#ff5f9e;--nv-glow:linear-gradient(135deg,#f2c230 0%,#ff8a5c 55%,#ff5f9e 100%);--nv-fd:'Bricolage Grotesque','Barlow',system-ui,sans-serif}\n#view-revista>.panel{background:transparent;border:0;backdrop-filter:none;-webkit-backdrop-filter:none;padding:4px 2px 8px}\n#view-revista .ic{width:18px;height:18px;color:currentColor}\n.rv-top .ptitle{font-family:var(--nv-fd);font-weight:800;letter-spacing:-.02em;font-size:30px;text-transform:none}\n.rv-me .rv-av{box-shadow:0 0 0 2px #1b1e23,0 0 0 4px transparent;background-image:var(--nv-glow)}\n/* Barra de secciones */\n.nv-dock{display:flex;gap:4px;padding:5px;margin:4px 0 12px;background:var(--nv-s1);border:1px solid var(--nv-bd);border-radius:20px;box-shadow:0 8px 24px rgba(0,0,0,.25)}\n.nv-di{flex:1 1 0;min-width:0;display:flex;flex-direction:column;align-items:center;gap:3px;padding:8px 2px 7px;background:transparent;border:0;border-radius:15px;color:var(--muted);font:600 11.5px/1.1 'Barlow',sans-serif;min-height:0;transition:background .2s,color .2s,transform .15s}\n.nv-di .ic{width:21px;height:21px}\n.nv-di span{white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:100%}\n.nv-di.on{background:var(--nv-glow);color:#1b1e23;box-shadow:0 4px 14px rgba(255,138,92,.35)}\n.nv-di:active{transform:scale(.95)}\n.nv-tabs{flex-wrap:nowrap;overflow-x:auto;scrollbar-width:none;gap:4px;border-bottom:1px solid var(--nv-bd);margin:0 0 14px;padding-bottom:0}\n.nv-tabs::-webkit-scrollbar{display:none}\n.nv-tabs .rv-tab{flex:none;border:0;border-radius:0;background:none;padding:10px 12px;font-weight:600;color:var(--muted);border-bottom:2.5px solid transparent;margin-bottom:-1px}\n.nv-tabs .rv-tab.on{color:var(--text);border-bottom-color:var(--nv-warm);background:none}\n/* Men\u00fa \u00abM\u00e1s\u00bb */\n.nv-sheet{position:fixed;inset:0;z-index:60;display:flex;align-items:flex-end;justify-content:center}\n.nv-shbg{position:absolute;inset:0;background:rgba(8,9,12,.55);backdrop-filter:blur(3px);-webkit-backdrop-filter:blur(3px)}\n.nv-shc{position:relative;width:100%;max-width:520px;background:#202229;border-radius:26px 26px 0 0;border:1px solid var(--nv-bd);padding:10px 16px calc(18px + env(safe-area-inset-bottom,0px));box-shadow:0 -10px 40px rgba(0,0,0,.45)}\n.nv-grab{width:42px;height:5px;border-radius:3px;background:rgba(255,255,255,.18);margin:2px auto 10px}\n.nv-shk{margin:0 4px 12px;font:800 20px var(--nv-fd)}\n.nv-tiles{display:grid;grid-template-columns:repeat(3,1fr);gap:10px}\n.nv-tile{display:flex;flex-direction:column;align-items:center;gap:8px;padding:14px 6px;border-radius:18px;background:var(--nv-s2);border:1px solid var(--nv-bd);color:var(--text);font:600 13px 'Barlow',sans-serif;min-height:0}\n#view-revista .nv-tile .ic{width:24px;height:24px;color:var(--line)}\n.nv-tile:active{transform:scale(.97)}\n.nv-shx{width:100%;margin-top:14px}\n@media (prefers-reduced-motion:no-preference){.nv-shc{animation:nvUp .26s cubic-bezier(.2,.9,.3,1.1)}.nv-shbg{animation:nvFade .2s}}\n@keyframes nvUp{from{transform:translateY(40px);opacity:.4}to{transform:none;opacity:1}}\n@keyframes nvFade{from{opacity:0}to{opacity:1}}\n/* Inicio */\n.nv-hello{margin:6px 2px 16px}\n.nv-day{margin:0;font-size:12px;letter-spacing:.12em;text-transform:uppercase;color:var(--muted)}\n.nv-hello h2{margin:4px 0 2px;font:800 28px/1.1 var(--nv-fd);letter-spacing:-.02em;text-wrap:balance}\n.nv-hello h2 span{background:var(--nv-glow);-webkit-background-clip:text;background-clip:text;color:transparent}\n.nv-hello p{margin:0;color:var(--muted)}\n.rv-stories{gap:16px;padding:6px 2px 14px}\n.rv-story .rv-av{box-shadow:0 0 0 2.5px #1b1e23,0 0 0 5px #ff8a5c;background-image:var(--nv-glow)}\n.nv-vb{display:grid;grid-template-columns:118px 1fr;gap:0;width:100%;padding:0;margin:0 0 18px;border:1px solid rgba(242,194,48,.25);border-radius:20px;overflow:hidden;background:linear-gradient(120deg,#2a2419,#26232e);text-align:left;color:var(--text);min-height:0;font-weight:400}\n.nv-vbimg{position:relative;display:block;background:#000}\n.nv-vbimg img{width:100%;height:100%;object-fit:cover;display:block}\n.nv-vbimg .bv-noimg{font-size:40px}\n.nv-vbtx{display:flex;flex-direction:column;gap:3px;padding:12px 14px;min-width:0}\n.nv-vbk{display:inline-flex;align-items:center;gap:5px;font-size:11px;letter-spacing:.1em;text-transform:uppercase;color:var(--line);font-weight:700}\n.nv-vbk .ic{width:14px;height:14px}\n.nv-vbtx b{font:800 18px/1.15 var(--nv-fd)}\n.nv-vbtx small{color:var(--muted);font-size:12.5px}\n.nv-vbgo{display:inline-flex;align-items:center;gap:5px;align-self:flex-start;margin-top:4px;font-size:13px;font-weight:700;color:#1b1e23;background:var(--line);border-radius:999px;padding:4px 11px}\n.nv-vbgo .ic{width:13px;height:13px}\n.nv-row{margin:4px 0 18px}\n.nv-rowh{display:flex;align-items:baseline;justify-content:space-between;gap:10px;margin:8px 2px 10px}\n.nv-rowh h4{margin:0;font:800 19px var(--nv-fd);letter-spacing:-.01em}\n.nv-more{background:none;border:0;padding:4px 0;color:var(--line);font-weight:700;font-size:14px;min-height:0}\n.nv-hscroll{display:flex;gap:12px;overflow-x:auto;scroll-snap-type:x mandatory;padding:2px 2px 8px;scrollbar-width:none}\n.nv-hscroll::-webkit-scrollbar{display:none}\n.nv-pst{flex:none;width:132px;scroll-snap-align:start;display:flex;flex-direction:column;gap:4px;padding:0;background:none;border:0;text-align:left;color:var(--text);min-height:0;font-weight:400}\n.nv-pst img{width:132px;height:176px;object-fit:cover;border-radius:14px;background:var(--nv-s2);box-shadow:0 6px 16px rgba(0,0,0,.35);display:block}\n.nv-pst b{font-size:13.5px;line-height:1.2;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;margin-top:2px}\n.nv-pst small{font-size:12px;color:var(--muted)}\n.rv-people.nv-hscroll{display:flex}\n.rv-people.nv-hscroll .rv-pc{flex:none;width:150px;scroll-snap-align:start;background:var(--nv-s1);border:1px solid var(--nv-bd);border-radius:20px}\n/* Tarjetas del inicio */\n.rv-grid{grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:16px;align-items:start}\n.nv-card{border-radius:22px;border:1px solid var(--nv-bd);box-shadow:0 6px 20px rgba(0,0,0,.22);transition:transform .18s,box-shadow .18s}\n.nv-card:not([data-th]),.nv-card[data-th=\"\"],.nv-card[data-th=asfalto]{background:var(--nv-s1)}\n.nv-card:hover{transform:translateY(-2px);box-shadow:0 10px 26px rgba(0,0,0,.3)}\n.nv-card .rv-in{padding:12px 16px 14px}\n.nv-card .nv-ctop{padding:14px 16px 10px}\n.nv-cau{display:flex;align-items:center;gap:10px}\n.nv-cwho{display:inline-flex;align-items:center;gap:10px;color:inherit;min-width:0}\n.nv-cwho>span{display:flex;flex-direction:column;line-height:1.15;min-width:0}\n.nv-cwho b{font-size:14.5px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}\n.nv-cwho small{font-size:12px;color:var(--rm,var(--muted))}\n.nv-cimg{margin:0 12px;border-radius:16px;overflow:hidden}\n.nv-cimg img{width:100%;aspect-ratio:16/9;object-fit:cover;display:block}\n.nv-card h3{font-family:var(--rfh,var(--nv-fd));font-size:calc(23px*var(--rhs,1));line-height:1.12;margin:2px 0 6px;letter-spacing:-.01em}\n.nv-card[data-ft=cartel] h3,.nv-card:not([data-ft]) h3{font-family:var(--nv-fd);font-weight:800}\n.nv-card .rv-sub{margin:0 0 8px;font-size:15px;line-height:1.4}\n.nv-card.hero h3{font-size:calc(28px*var(--rhs,1))}\n.nv-cacts{display:flex;gap:16px;align-items:center;margin-top:10px;padding-top:10px;border-top:1px solid rgba(127,127,127,.18);font-size:14px;color:var(--rm,var(--muted))}\n.nv-cacts span{display:inline-flex;align-items:center;gap:6px}\n.nv-cacts .ic{width:17px;height:17px}\n.nv-card.mag .nv-ctop{padding-bottom:12px}\n/* Art\u00edculo */\n.nv-by{display:flex;align-items:center;gap:12px;margin:14px 0}\n.nv-by>span{display:flex;flex-direction:column;line-height:1.2;margin-right:auto;min-width:0}\n.nv-by b{font-size:15.5px}.nv-by small{font-size:13px;color:var(--rm,var(--muted))}\n.nv-by .rv-av{box-shadow:0 0 0 2px rgba(0,0,0,.3)}\n.nv-pill{position:sticky;bottom:calc(92px + env(safe-area-inset-bottom,0px));z-index:5;display:flex;justify-content:center;gap:4px;width:max-content;max-width:100%;margin:22px auto 4px;padding:6px;border-radius:999px;background:rgba(30,32,38,.92);backdrop-filter:blur(14px);-webkit-backdrop-filter:blur(14px);border:1px solid rgba(255,255,255,.1);box-shadow:0 10px 30px rgba(0,0,0,.45)}\n.nv-pill button{display:inline-flex;align-items:center;gap:7px;background:none;border:0;border-radius:999px;color:#f1f2f4;padding:9px 15px;font-weight:600;font-size:15px;min-height:0}\n.nv-pill button .ic{width:19px;height:19px}\n.nv-pill button.on{color:var(--nv-pink)}\n.nv-pill button.on .ic{fill:currentColor}\n.nv-pill button:active{background:rgba(255,255,255,.08)}\n/* Perfil */\n.nv-prof{padding:0!important;overflow:hidden;border-radius:24px!important;border:1px solid var(--nv-bd)}\n.nv-prof:not([data-th]),.nv-prof[data-th=asfalto]{background:var(--nv-s1)}\n.nv-ban{position:relative;height:150px;background:#111}\n.nv-ban img{width:100%;height:100%;object-fit:cover;display:block}\n.nv-ban0,.nv-banprev .nv-ban0{background:radial-gradient(120% 140% at 10% 0%,hsl(var(--h) 85% 62% / .95),transparent 60%),radial-gradient(120% 140% at 100% 100%,hsl(calc(var(--h) + 70) 80% 58% / .9),transparent 60%),linear-gradient(135deg,hsl(calc(var(--h) + 30) 60% 30%),#1b1e23)}\n.nv-pm{padding:0 18px 18px}\n.nv-prow{position:relative;z-index:2;display:flex;align-items:flex-end;justify-content:space-between;gap:10px;margin-top:-48px}\n.nv-pav .rv-av{box-shadow:0 0 0 4px var(--nv-s1),0 0 0 7px transparent;border:0;background-image:var(--nv-glow)}\n.nv-prof[data-th]:not([data-th=asfalto]) .nv-pav .rv-av{box-shadow:0 0 0 4px rgba(0,0,0,.25)}\n.nv-pact{display:flex;gap:8px;align-items:center;padding-bottom:6px}\n.nv-pact .rv-fol{padding:9px 20px;font-size:15px;font-weight:700}\n.nv-prof .rv-prof-top{margin-top:10px}\n.nv-prof h2{font-family:var(--rfh,var(--nv-fd));letter-spacing:-.01em}\n.nv-prof:not([data-ft]) h2,.nv-prof[data-ft=cartel] h2{font-family:var(--nv-fd);font-weight:800}\n.nv-stats{display:flex;flex-wrap:wrap;gap:6px 16px;margin:10px 0 4px;font-size:14.5px;color:var(--rm,var(--muted))}\n.nv-stats b{color:var(--rt,var(--text));font-size:16px}\n.nv-pbtns2:empty{display:none}\n.nv-ib{display:inline-flex;align-items:center;justify-content:center;width:40px;height:40px;padding:0;border-radius:50%;background:rgba(127,127,127,.16);border:1px solid rgba(127,127,127,.25);color:inherit;min-height:0}\n.nv-ib .ic{width:18px;height:18px}\n.nv-banprev{height:120px;border-radius:16px;overflow:hidden;margin:4px 0 8px;background:#111}\n.nv-banprev img,.nv-banprev .nv-ban0{width:100%;height:100%;object-fit:cover;display:block}\n/* Muro */\n.nv-wall{display:flex;flex-direction:column;gap:12px}\n.nv-comp{display:flex;gap:12px;padding:14px;border-radius:22px;background:var(--nv-s1);border:1px solid var(--nv-bd)}\n.nv-compb{flex:1;min-width:0;display:flex;flex-direction:column;gap:8px}\n.nv-compb textarea{width:100%;min-height:56px;resize:none;border-radius:14px;background:rgba(0,0,0,.18);border:1px solid var(--nv-bd);padding:10px 12px;font-size:15.5px;line-height:1.45}\n.nv-compa{display:flex;align-items:center;gap:10px}\n.nv-compa .rv-m{margin-right:auto;font-size:12px}\n.nv-compa>button:last-child{border-radius:999px;padding:9px 20px;font-weight:700}\n.nv-wprev{position:relative;display:inline-block}\n.nv-wprev img{max-height:180px;max-width:100%;border-radius:14px;display:block}\n.nv-wprev .nv-ib{position:absolute;top:6px;right:6px;width:32px;height:32px;background:rgba(0,0,0,.6)}\n.nv-note{padding:14px 16px;border-radius:22px;background:var(--nv-s1);border:1px solid var(--nv-bd)}\n.nv-nh{display:flex;align-items:flex-start;gap:10px}\n.nv-nh>a{flex:none}\n.nv-nwho{display:flex;flex-direction:column;line-height:1.2;margin-right:auto;min-width:0}\n.nv-nwho a,.nv-nh>a,.nv-by a,.nv-cwho{color:var(--text);text-decoration:none}.nv-nwho b{font-size:15px}\n.nv-to{font-size:12.5px;color:var(--muted)}\n.nv-nwho small{font-size:12px;color:var(--muted)}\n.nv-nh .nv-ib{width:34px;height:34px;background:transparent;border-color:transparent;color:var(--muted)}\n.nv-nb{margin:10px 0 0;font-size:16px;line-height:1.5;overflow-wrap:anywhere}\n.nv-nimg{width:100%;max-height:420px;object-fit:cover;border-radius:16px;margin-top:10px;display:block}\n.nv-nacts{display:flex;align-items:center;gap:12px;margin-top:10px}\n.nv-lk{display:inline-flex;align-items:center;gap:6px;background:none;border:0;padding:6px 10px 6px 0;color:var(--muted);font-weight:600;min-height:0}\n.nv-lk .ic{width:19px;height:19px}\n.nv-lk.on{color:var(--nv-pink)}.nv-lk.on .ic{fill:currentColor}\n@media (prefers-reduced-motion:no-preference){.nv-lk.on .ic{animation:nvPop .3s}}\n@keyframes nvPop{50%{transform:scale(1.3)}}\n.nv-wnote{margin:0}\n.nv-empty{display:flex;flex-direction:column;align-items:center;gap:8px;text-align:center;padding:28px 16px;border-radius:22px;border:1px dashed rgba(255,138,92,.4);color:var(--muted)}\n.nv-empty .ic{width:28px;height:28px;color:var(--nv-warm)}.nv-empty p{margin:0;max-width:36ch}\n/* Toques generales */\n#view-revista .rv-fab{border-radius:20px;background:var(--nv-glow);color:#1b1e23;box-shadow:0 10px 28px rgba(255,120,90,.45);border:0}\n#view-revista .rv-fol:not(.on){background:var(--nv-glow);color:#1b1e23;border:0}\n#view-revista .rv-head .alt{border-radius:999px}\n.rv-c{border-radius:16px}\n@media (min-width:700px){.nv-ban{height:200px}.nv-hello h2{font-size:34px}}\n/* Muro: respuestas, encuestas, dedicatorias, menciones */\n.nv-ib.on{background:var(--nv-glow);color:#1b1e23;border-color:transparent}\n.nv-pin{display:grid;grid-template-columns:1fr 1fr;gap:8px}.nv-pin input{min-width:0;border-radius:12px}\n.nv-dedhint{margin:0;font-size:13px;color:var(--nv-warm);display:flex;gap:6px;align-items:center}\n.nv-ded{margin:-2px 0 10px;display:inline-flex;align-items:center;gap:6px;font-size:12.5px;font-weight:700;color:#1b1e23;background:var(--nv-glow);border-radius:999px;padding:4px 11px}\n.nv-ded .ic{width:14px;height:14px}\n.nv-isded{border-color:rgba(255,138,92,.45);box-shadow:0 0 0 1px rgba(255,138,92,.15) inset}\n.nv-at,.nv-hash{color:var(--nv-warm);font-weight:600;text-decoration:none}\n.nv-replies{margin:10px 0 0 22px;padding-left:12px;border-left:2px solid rgba(255,255,255,.08);display:flex;flex-direction:column;gap:10px}\n.nv-reply .nv-nb{font-size:15px;margin-top:6px}.nv-reply .nv-nacts{margin-top:4px}\n.nv-rcomp{display:flex;gap:8px;align-items:flex-end;margin-top:10px}.nv-rcomp textarea{flex:1;min-width:0;border-radius:14px;min-height:44px}.nv-rcomp button{border-radius:999px}\n.nv-poll{display:flex;flex-direction:column;gap:6px;margin-top:10px}\n.nv-popt{position:relative;overflow:hidden;display:flex;align-items:center;justify-content:space-between;gap:10px;padding:10px 14px;border-radius:14px;background:rgba(255,255,255,.05);border:1px solid rgba(255,255,255,.12);color:var(--text);text-align:left;min-height:0;font-weight:600}\n.nv-popt.on{border-color:var(--nv-warm)}\n.nv-pbar{position:absolute;left:0;top:0;bottom:0;background:linear-gradient(90deg,rgba(242,194,48,.28),rgba(255,95,158,.22));transition:width .4s}\n.nv-ptx,.nv-popt b{position:relative}\n.nv-poll .rv-m{margin:2px 0 0;font-size:12.5px}\n/* Escuchando ahora */\n.nv-now{display:flex;align-items:center;gap:12px;width:100%;margin:10px 0 2px;padding:10px 14px;border-radius:16px;border:1px solid rgba(62,207,142,.3);background:linear-gradient(120deg,rgba(62,207,142,.12),rgba(255,255,255,.03));color:var(--rt,var(--text));text-align:left;min-height:0;font-weight:400}\n.nv-now .nv-nowt{display:flex;flex-direction:column;min-width:0;flex:1;line-height:1.25}\n.nv-now small{color:#3ecf8e;font-weight:700;font-size:11.5px;text-transform:uppercase;letter-spacing:.06em}\n.nv-now b{white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.nv-now .nv-nowt>span{font-size:13px;color:var(--rm,var(--muted))}\n.nv-eq{display:inline-flex;align-items:flex-end;gap:2px;height:18px;flex:none}.nv-eq i{width:3px;background:#3ecf8e;border-radius:2px;height:40%}\n@media (prefers-reduced-motion:no-preference){.nv-eq i{animation:nvEq 1s infinite ease-in-out}.nv-eq i:nth-child(2){animation-delay:-.4s}.nv-eq i:nth-child(3){animation-delay:-.7s}}\n@keyframes nvEq{50%{height:100%}}\n.nv-nfi{background:var(--nv-glow)!important;color:#1b1e23}\n/* Club de escucha */\n.cl-mast{background:radial-gradient(120% 140% at 100% 0%,rgba(255,95,158,.18),transparent 55%),linear-gradient(160deg,#141218,#1e1a24 60%,#251a14);border-color:rgba(255,95,158,.25)}\n.cl-card{padding:16px;border-radius:22px;background:var(--nv-s1);border:1px solid var(--nv-bd);margin-bottom:12px}\n.cl-head{display:flex;gap:14px;align-items:center}\n.cl-head .cl-cov,.cl-head .cl-noc{width:116px;height:116px;border-radius:14px;object-fit:cover;flex:none;box-shadow:0 8px 20px rgba(0,0,0,.4)}\n.cl-noc{display:flex;align-items:center;justify-content:center;font:800 48px var(--nv-fd);color:#fff;background:linear-gradient(135deg,hsl(var(--h) 70% 45%),hsl(calc(var(--h) + 60) 70% 30%))}\n.nv-vbimg .cl-noc{position:absolute;inset:0;font-size:40px}\n.cl-pst .cl-noc,.cl-pst img{width:132px;height:132px;border-radius:14px}\n.cl-k{margin:0;font-size:11.5px;letter-spacing:.1em;text-transform:uppercase;color:var(--line);font-weight:700}\n.cl-head h3{margin:4px 0 2px;font:800 26px/1.1 var(--nv-fd)}.cl-a{margin:0;color:var(--muted)}\n.cl-note{margin-top:12px}\n.cl-tracks{display:flex;flex-direction:column;gap:8px;margin-bottom:16px}\n.cl-tr{border-radius:16px;background:var(--nv-s1);border:1px solid var(--nv-bd);overflow:hidden}\n.cl-tr summary{display:flex;align-items:center;gap:12px;padding:12px 14px;cursor:pointer;list-style:none}\n.cl-tr summary::-webkit-details-marker{display:none}\n.cl-n{width:26px;height:26px;border-radius:50%;background:rgba(255,255,255,.08);display:inline-flex;align-items:center;justify-content:center;font-size:13px;font-weight:700;flex:none}\n.cl-tn{flex:1;min-width:0;font-weight:600}.cl-cc{display:inline-flex;align-items:center;gap:5px;color:var(--muted);font-size:13px}\n.cl-tr[open] .cl-n{background:var(--nv-glow);color:#1b1e23}\n.cl-th{padding:0 14px 14px}.cl-th textarea{width:100%;margin-top:6px}.cl-th button{margin-top:6px}\n.cl-ban{border-color:rgba(255,95,158,.3);background:linear-gradient(120deg,#2a1a24,#22232c)}\n/* Funci\u00f3n en vivo */\n.bv-live{display:flex;flex-wrap:wrap;align-items:center;gap:6px 10px;margin:6px 0 8px;padding:10px 14px;border-radius:14px;background:rgba(229,96,77,.12);border:1px solid rgba(229,96,77,.35);font-size:14px}\n.bv-live b{letter-spacing:.04em}.bv-live.on{background:rgba(229,96,77,.22)}\n.bv-dot{width:10px;height:10px;border-radius:50%;background:#e5604d;box-shadow:0 0 0 4px rgba(229,96,77,.25)}\n@media (prefers-reduced-motion:no-preference){.bv-live.on .bv-dot{animation:bvPulse 1.2s infinite}}\n@keyframes bvPulse{50%{box-shadow:0 0 0 8px rgba(229,96,77,0)}}\n\n.sv-list{display:flex;flex-direction:column;gap:10px}\n.sv-row{display:flex;align-items:center;gap:12px;padding:10px;border-radius:16px;background:rgba(255,255,255,.04);border:1px solid rgba(255,255,255,.07)}\n.sv-row img,.sv-ph{width:64px;height:64px;flex:none;border-radius:12px;object-fit:cover}\n.sv-ph{display:flex;align-items:center;justify-content:center;background:linear-gradient(135deg,hsl(var(--h) 70% 55%),hsl(calc(var(--h) + 50) 70% 45%))}\n.sv-ph .ic{width:26px;height:26px;color:#fff}\n.sv-row>div{flex:1;min-width:0}.sv-row b{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.sv-row small{color:var(--muted)}\n#svBtn.on{background:rgba(62,207,142,.16);border-color:rgba(62,207,142,.5)}\n.ls-mast{background:linear-gradient(135deg,#3a2a6b,#1f6f8b)}\n.ls-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:12px;margin-top:12px}\n.ls-card{display:flex;flex-direction:column;align-items:flex-start;gap:4px;padding:12px;border-radius:18px;text-align:left;background:rgba(255,255,255,.04);border:1px solid rgba(255,255,255,.08);color:var(--text)}\n.ls-card b{font-size:16px;line-height:1.2}.ls-card small{color:var(--muted);font-size:13px}\n.ls-art{width:100%;aspect-ratio:1;border-radius:14px;display:flex;align-items:center;justify-content:center;background:linear-gradient(135deg,hsl(var(--h) 75% 58%),hsl(calc(var(--h) + 70) 70% 42%));margin-bottom:6px}\n.ls-art .ic{width:40%;height:40%;color:#fff;opacity:.9}\n.ls-head{display:flex;gap:14px;align-items:center;margin:4px 0 12px}\n.ls-head h3{margin:2px 0 4px;font-family:'Barlow Condensed',sans-serif;font-size:30px;line-height:1.05}\n.ls-big{width:110px;flex:none;margin:0}\n.ls-tr{gap:10px}.ls-tr>div{flex:1;min-width:0}\n.ls-head a{color:inherit;text-decoration:none}.ls-head a:hover{text-decoration:underline}\n.ls-lk{display:inline-flex;align-items:center;flex:none;padding:8px 14px;border-radius:999px;text-decoration:none;font-size:14px;color:var(--text);border:1px solid rgba(255,255,255,.18);background:rgba(255,255,255,.06)}\nhtml[data-theme=light] .ls-lk{border-color:rgba(0,0,0,.15);background:rgba(0,0,0,.04)}\n.ls-add{margin-top:16px}\nhtml[data-theme=light] .sv-row,html[data-theme=light] .ls-card{background:#fff;border-color:rgba(0,0,0,.08)}\n/* Nombres y textos largos (también sin espacios): se parten o se recortan con «…», nunca se salen de su casilla */\n#view-revista,.rv-ov,.nv-sheet,.rv-lb,.rv-pdet{overflow-wrap:anywhere}\n.rv-pc>*{max-width:100%;min-width:0}\n.rv-pc b,.rv-pc small{overflow:hidden;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;word-break:break-word}\n.rv-like{max-width:100%;white-space:normal}\n.rv-by>span,.nv-by>span,.nv-prow>*,.nv-pact{min-width:0}\n" + '</style>');
/* ---------- Revista: artículos, perfiles, artistas y pósters · servidor Supabase ---------- */
// REV_URL y REV_KEY (proyecto de Supabase y su clave pública) están en index.html: los usa también la Bóveda en la vista de artista.
const REV_PAGE = 12, UUID = /^[0-9a-f-]{36}$/i;
const revCfg = () => ({ url: (setting('sbUrl', '') || (revMine() ? REV_URL : '')).replace(/\/+$/, ''), key: setting('sbKey', '') || (revMine() ? REV_KEY : '') });
const revOk = () => { const c = revCfg(); return /^https:\/\/[a-z0-9-]+\.supabase\.co$/i.test(c.url) && c.key.length > 20; };
let revSes;
const revMe = () => {
  if (revSes === undefined) { try { revSes = JSON.parse(localStorage.getItem('rr_rev_ses') || 'null'); } catch { revSes = null; } }
  return revSes && revSes.uid ? revSes : null;
};
const revSaveSes = s => { revSes = s; try { s ? localStorage.setItem('rr_rev_ses', JSON.stringify(s)) : localStorage.removeItem('rr_rev_ses'); } catch {} };
const revFromAuth = d => revSaveSes({ access: d.access_token, refresh: d.refresh_token, exp: Date.now() + (d.expires_in || 3600) * 1000, uid: d.user.id, name: ((d.user.user_metadata || {}).name || '').slice(0, 40) });
let revRefP = null;
function revRefresh() {
  return revRefP || (revRefP = (async () => {
    const c = revCfg(), s = revMe(); if (!s || !s.refresh) return;
    try {
      const r = await fetch(c.url + '/auth/v1/token?grant_type=refresh_token', { method: 'POST', headers: { apikey: c.key, 'Content-Type': 'application/json' }, body: JSON.stringify({ refresh_token: s.refresh }) });
      if (r.ok) { const d = await r.json(); const nm = (revMe() || {}).name; revFromAuth(d); if (nm && revSes) revSes.name = nm; } else if (r.status === 400 || r.status === 401) revSaveSes(null);
    } catch {}
  })().finally(() => { revRefP = null; }));
}
const V2_MSG = 'Falta actualizar la base de datos: ejecuta revista_supabase_v2.sql en Supabase (SQL Editor).';
const revErr = m => {
  m = String(m || '');
  if (/Invalid login/i.test(m)) return 'Correo o contraseña incorrectos.';
  if (/already registered|already been registered/i.test(m)) return 'Ese correo ya tiene cuenta. Usa «Entrar».';
  if (/Email not confirmed/i.test(m)) return 'Confirma tu correo (te llegó un mensaje) y vuelve a entrar.';
  if (/Password should be/i.test(m)) return 'La contraseña debe tener al menos 6 caracteres.';
  if (/Failed to fetch|NetworkError|Load failed/i.test(m)) return 'Sin conexión con el servidor de la revista.';
  if (/Solo el administrador/i.test(m)) return 'Solo el administrador puede cambiar permisos.';
  if (/Límite alcanzado|suspendida|invitación incorrecto/i.test(m)) return m;
  if (/Database error saving new user/i.test(m)) return 'No se pudo crear la cuenta. Si la revista pide código de invitación, revisa que sea correcto.';
  if (/duplicate key|already exists|Duplicate/i.test(m)) return 'Eso ya existe (nombre o imagen repetidos).';
  if (/does not exist|schema cache|Could not find|Bucket not found/i.test(m)) return V2_MSG;
  if (/mime type|not supported/i.test(m)) return 'Ese formato de imagen no está permitido (usa JPG, PNG o WEBP).';
  if (/maximum allowed size|too large|Payload/i.test(m)) return 'La imagen es demasiado pesada.';
  if (/row-level security|violates/i.test(m)) return 'No tienes permiso para hacer eso.';
  return m || 'Algo salió mal.';
};
async function revTok() {
  const s = revMe();
  if (s && s.exp && Date.now() > s.exp - 60000) await revRefresh();
  return (revMe() || {}).access || '';
}
async function revFetch(path, o = {}) {
  const c = revCfg(), tok = await revTok();
  const h = { apikey: c.key, 'Content-Type': 'application/json' };
  if (tok) h.Authorization = 'Bearer ' + tok; // sin sesión solo va la clave pública
  if (o.prefer) h.Prefer = o.prefer;
  if (o.count) h.Prefer = 'count=exact';
  let r;
  try { r = await fetch(c.url + path, { method: o.method || 'GET', headers: h, body: o.body ? JSON.stringify(o.body) : undefined }); }
  catch (e) { throw new Error(revErr(e.message)); }
  const t = await r.text(); let data = null; try { data = t ? JSON.parse(t) : null; } catch { data = null; }
  if (!r.ok) { const raw = (data && (data.message || data.msg || data.error_description || data.hint)) || 'Error ' + r.status; const e = new Error(revErr(raw)); e.status = r.status; e.raw = raw; throw e; }
  if (o.count) { const cr = r.headers.get('content-range') || ''; return +(cr.split('/')[1]) || 0; }
  return data;
}
async function revAuth(mode, email, pass, name, invite) {
  const c = revCfg(); if (mode === 'signup') try { localStorage.setItem('rr_last_auth', 'signup'); } catch {}
  let r;
  try {
    r = await fetch(c.url + (mode === 'signup' ? '/auth/v1/signup?redirect_to=' + encodeURIComponent(location.href.split('#')[0]) : '/auth/v1/token?grant_type=password'), { method: 'POST', headers: { apikey: c.key, 'Content-Type': 'application/json' }, body: JSON.stringify(mode === 'signup' ? { email, password: pass, data: invite ? { name, invite, rules: '2026-11' } : { name, rules: '2026-11' } } : { email, password: pass }) });
  } catch (e) { throw new Error(revErr(e.message)); }
  const d = await r.json().catch(() => ({}));
  if (!r.ok) throw new Error(revErr(d.msg || d.message || d.error_description || d.error));
  if (d.access_token && d.user) { revFromAuth(d); return true; }
  return false; // el proyecto pide confirmar el correo
}
// Sube una imagen al almacén «revista», dentro de la carpeta de quien la sube.
async function revUpload(blob, file) {
  const c = revCfg(), tok = await revTok(), me = revMe();
  if (!me || !tok) throw new Error('Entra a tu cuenta para subir imágenes.');
  const path = `${me.uid}/${file}`;
  let r;
  try { r = await fetch(`${c.url}/storage/v1/object/revista/${path}`, { method: 'POST', headers: { apikey: c.key, Authorization: 'Bearer ' + tok, 'Content-Type': blob.type || 'image/jpeg', 'x-upsert': 'false' }, body: blob }); }
  catch (e) { throw new Error(revErr(e.message)); }
  if (!r.ok) { const d = await r.json().catch(() => ({})); throw new Error(revErr(d.message || d.error || 'No se pudo subir la imagen.')); }
  return `${c.url}/storage/v1/object/public/revista/${path}`;
}
async function revUnupload(url) { // borra del almacén (mejor esfuerzo)
  const c = revCfg(), m = /\/storage\/v1\/object\/public\/revista\/(.+)$/.exec(url || ''); if (!m) return;
  try { const tok = await revTok(); await fetch(`${c.url}/storage/v1/object/revista/${m[1]}`, { method: 'DELETE', headers: { apikey: c.key, Authorization: 'Bearer ' + tok } }); } catch {}
}
// Reduce una imagen a JPEG. square = recorte cuadrado (fotos de perfil).
async function revResize(file, max, q = 0.82, square = false) {
  const url = URL.createObjectURL(file), img = new Image(); img.src = url;
  try { await img.decode(); } finally { URL.revokeObjectURL(url); }
  let sx = 0, sy = 0, sw = img.naturalWidth, sh = img.naturalHeight;
  if (square) { const m = Math.min(sw, sh); sx = (sw - m) / 2; sy = (sh - m) / 2; sw = sh = m; }
  const k = Math.min(1, max / Math.max(sw, sh)), c = document.createElement('canvas');
  c.width = Math.max(1, Math.round(sw * k)); c.height = Math.max(1, Math.round(sh * k));
  const g = c.getContext('2d'); g.fillStyle = '#fff'; g.fillRect(0, 0, c.width, c.height); g.drawImage(img, sx, sy, sw, sh, 0, 0, c.width, c.height);
  const blob = await new Promise(ok => c.toBlob(ok, 'image/jpeg', q));
  return { blob, data: () => c.toDataURL('image/jpeg', q) };
}
const revUid = () => (crypto.randomUUID ? crypto.randomUUID() : Date.now().toString(36) + Math.random().toString(36).slice(2));
async function revDownload(url, name) {
  try {
    const r = await fetch(url); if (!r.ok) throw 0; const b = await r.blob();
    const ext = /png/.test(b.type) ? 'png' : /webp/.test(b.type) ? 'webp' : 'jpg';
    const u = URL.createObjectURL(b), a = document.createElement('a');
    a.href = u; a.download = (String(name || 'poster').replace(/[^\w\- áéíóúñü]+/gi, '').trim().slice(0, 60) || 'poster') + '.' + ext;
    document.body.appendChild(a); a.click(); a.remove(); setTimeout(() => URL.revokeObjectURL(u), 5000); toast('Descargando…');
  } catch { window.open(url, '_blank', 'noopener'); }
}
async function revShare(title, hash) {
  const url = location.href.split('#')[0] + hash;
  try { if (navigator.share) { await navigator.share({ title, url }); return; } } catch (e) { if (e && e.name === 'AbortError') return; }
  try { await navigator.clipboard.writeText(url); toast('Enlace copiado.'); } catch { toast(url, false, 8000); }
}
const revTxt = h => { try { const d = new DOMParser().parseFromString(String(h), 'text/html'); d.querySelectorAll('script,style,noscript').forEach(x => x.remove()); return (d.body.textContent || '').replace(/\s+/g, ' ').trim(); } catch { return ''; } };
const revCnt = a => (a && a[0] && a[0].count) || 0;
const revDate = (s, o = { day: 'numeric', month: 'short', year: 'numeric' }) => { try { return new Date(s).toLocaleDateString('es', o); } catch { return ''; } };
const revImg = u => (typeof u === 'string' && /^data:image\/jpeg;base64,[A-Za-z0-9+/=]+$/.test(u)) ? u : '';
// Solo imágenes de tu almacén o de Wikimedia (nada de enlaces externos arbitrarios).
const revUrl = u => (typeof u === 'string' && (/^https:\/\/[a-z0-9-]+\.supabase\.co\/storage\/v1\/object\/public\/revista\/[\w\/.-]+$/i.test(u) || /^https:\/\/upload\.wikimedia\.org\/[^\s"'<>]+$/.test(u))) ? u : '';
const revPic = u => revImg(u) || revUrl(u);
// Imágenes en el almacén: grande + miniatura «-t.jpg» (el feed usa la miniatura; si no existe, la vista cae a la grande).
const revThumb = u => /\/storage\/v1\/object\/public\/revista\/.+\.jpg$/.test(u || '') && !/-t\.jpg$/.test(u) ? u.replace(/\.jpg$/, '-t.jpg') : u;
const revDataBlob = d => { const m = /^data:([^;]+);base64,(.*)$/.exec(d || ''); if (!m) return null; const b = atob(m[2]), u8 = new Uint8Array(b.length); for (let i = 0; i < b.length; i++) u8[i] = b.charCodeAt(i); return new Blob([u8], { type: m[1] }); };
async function revUploadPair(src, name, big, small) {
  const blob = typeof src === 'string' ? revDataBlob(src) : src; if (!blob) throw new Error('Imagen inválida.');
  const file = blob instanceof File ? blob : new File([blob], 'i.jpg', { type: blob.type || 'image/jpeg' });
  const [a, b] = await Promise.all([revResize(file, big, 0.84), revResize(file, small, 0.78)]);
  const url = await revUpload(a.blob, name + '.jpg');
  try { await revUpload(b.blob, name + '-t.jpg'); } catch {} // la miniatura es opcional
  return url;
}
const revUnPair = u => { if (!revUrl(u) || !/\.supabase\.co\//.test(u)) return; revUnupload(u); if (revThumb(u) !== u) revUnupload(revThumb(u)); };
const revImgTag = (u, cls = '', alt = '') => { const im = revPic(u); if (!im) return ''; const t = revThumb(im); return `<img${cls ? ` class="${cls}"` : ''} src="${esc(t)}"${t !== im ? ` data-full="${esc(im)}"` : ''} alt="${esc(alt)}" loading="lazy" decoding="async">`; };
const revNk = s => String(s || '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/[^a-z0-9]+/g, ' ').trim();
const revPat = s => String(s || '').replace(/[",()*\\:.]/g, ' ').replace(/\s+/g, ' ').trim().slice(0, 60);
const revName = p => (p && p.profiles && p.profiles.name) || (p && p.name) || 'Anónimo';
const revAv = (p, n = 40) => { p = p || {}; const im = revPic(p.avatar), nm = p.name || (p.profiles && p.profiles.name) || '?'; return im ? `<img class="rv-av" src="${esc(im)}" alt="" width="${n}" height="${n}" loading="lazy" decoding="async" style="width:${n}px;height:${n}px">` : `<span class="rv-av rv-av0" style="width:${n}px;height:${n}px;font-size:${Math.round(n * .45)}px">${esc(nm.trim().slice(0, 1).toUpperCase() || '?')}</span>`; };
const ICO = n => `<svg class="ic" aria-hidden="true"><use href="#i-${n}"/></svg>`;
function revFmt(t) {
  const inl = s => esc(s).replace(/\*\*([^*\n]+)\*\*/g, '<strong>$1</strong>').replace(/\*([^*\n]+)\*/g, '<em>$1</em>').replace(/https?:\/\/[^\s<]+/g, u => `<a href="${u}" target="_blank" rel="noopener noreferrer">${u}</a>`);
  return String(t || '').replace(/\r/g, '').split(/\n{2,}/).map(b => {
    b = b.trim(); if (!b) return '';
    const xb = typeof revBlock === 'function' ? revBlock(b, inl) : null; if (xb) return xb;
    if (b.startsWith('## ')) return `<h4>${inl(b.slice(3))}</h4>`;
    if (b.startsWith('# ')) return `<h3>${inl(b.slice(2))}</h3>`;
    const ls = b.split('\n');
    if (ls.every(l => l.startsWith('> '))) return `<blockquote>${ls.map(l => inl(l.slice(2))).join('<br>')}</blockquote>`;
    if (ls.every(l => l.startsWith('- '))) return `<ul>${ls.map(l => `<li>${inl(l.slice(2))}</li>`).join('')}</ul>`;
    return `<p>${ls.map(inl).join('<br>')}</p>`;
  }).join('');
}

/* --- Estéticas: solo valores de listas permitidas (nadie puede meter CSS ni HTML por aquí) --- */
const REV_TH = { asfalto: 'Asfalto', senal: 'Señal', papel: 'Papel', neon: 'Neón', atardecer: 'Atardecer', vinilo: 'Vinilo',
  y2k: 'Pop Y2K', punk: 'Fanzine punk', clasica: 'Rock clásico', vintage: 'Cartel vintage', teen: 'Teen 2002', tokio: 'Tokio kawaii', tabloide: 'Tabloide', cromo: 'Cromo 2000' };
const REV_THC = { asfalto: '#1b1e23', senal: '#1b4f9c', papel: '#f1e9d8', neon: '#0b0a14', atardecer: '#b4452f', vinilo: '#0e0e0e',
  y2k: 'linear-gradient(135deg,#b6f23a,#29d3f0 60%,#ff8a3d)', punk: '#0b0b0b', clasica: '#f6f3ee', vintage: '#efe4cc', teen: 'linear-gradient(90deg,#f2711c 50%,#3d5afe 50%)', tokio: '#ffd6e8', tabloide: 'repeating-linear-gradient(0deg,#1d4ed8 0 3px,#fff 3px 6px)', cromo: 'linear-gradient(160deg,#e9eef5,#9fb3c8 50%,#f4f7fb)' };
const REV_LIGHT = new Set(['papel', 'y2k', 'clasica', 'vintage', 'teen', 'tokio', 'tabloide', 'cromo']);
const REV_FT = { cartel: 'Cartel', editorial: 'Editorial', maquina: 'Máquina', manuscrita: 'Manuscrita', afiche: 'Afiche', poster: 'Póster', retro: 'Retro', burbuja: 'Burbuja', glitch: 'Glitch', bloque: 'Bloque' };
const REV_AC = { amarillo: '#f2c230', azul: '#4f8ff0', coral: '#ff6b5a', verde: '#3ecf8e', violeta: '#a78bfa', rojo: '#e3242b', rosa: '#ff3d8b', tinta: '#9aa3b0' };
// Al elegir una estética de revista se ajustan tipografía y acento (luego se pueden cambiar).
const REV_PRE = { y2k: { ft: 'retro', ac: 'rosa' }, punk: { ft: 'poster', ac: 'rojo' }, clasica: { ft: 'editorial', ac: 'rojo' }, vintage: { ft: 'poster', ac: 'rojo' }, teen: { ft: 'bloque', ac: 'coral' }, tokio: { ft: 'burbuja', ac: 'rosa' }, tabloide: { ft: 'bloque', ac: 'azul' }, cromo: { ft: 'glitch', ac: 'azul' } };
const REV_DEF = { th: 'asfalto', ft: 'cartel', ac: 'amarillo', al: 'l', dc: 0, sz: 'm', cv: 'a', stk: '', mh: '' };
const revClean = (s, n) => typeof s === 'string' ? s.replace(/[\u0000-\u001f<>]/g, '').trim().slice(0, n) : '';
const revSt = (o, base) => {
  o = o && typeof o === 'object' ? o : {}; const r = { ...REV_DEF, ...(base || {}) };
  if (typeof o.th === 'string' && Object.hasOwn(REV_TH, o.th)) r.th = o.th;
  if (typeof o.ft === 'string' && Object.hasOwn(REV_FT, o.ft)) r.ft = o.ft;
  if (typeof o.ac === 'string' && Object.hasOwn(REV_AC, o.ac)) r.ac = o.ac;
  if (o.al === 'c' || o.al === 'l') r.al = o.al; if (o.dc === 1 || o.dc === 0) r.dc = o.dc; if (o.sz === 'g' || o.sz === 'm') r.sz = o.sz;
  if (o.cv === 'f' || o.cv === 'm' || o.cv === 'a') r.cv = o.cv;
  if (typeof o.stk === 'string') r.stk = revClean(o.stk, 24); if (typeof o.mh === 'string') r.mh = revClean(o.mh, 22);
  return r;
};
const revAttrs = st => `data-th="${st.th}" data-ft="${st.ft}" data-ac="${st.ac}" data-al="${st.al}" data-dc="${st.dc}" data-sz="${st.sz}" data-cv="${st.cv}" data-lt="${REV_LIGHT.has(st.th) ? 1 : 0}"`;
const revStDiff = (st, base) => { const b = { ...REV_DEF, ...(base || {}) }, d = {}; Object.keys(REV_DEF).forEach(k => { if (st[k] !== b[k]) d[k] = st[k]; }); return d; };
let revFontsOn = false;
function revFonts() {
  if (revFontsOn) return; revFontsOn = true;
  const l = document.createElement('link'); l.rel = 'stylesheet';
  l.href = 'https://fonts.googleapis.com/css2?family=Playfair+Display:wght@700&family=Lora:wght@400;500&family=Space+Mono:wght@400;700&family=Caveat:wght@600&family=Abril+Fatface&family=Anton&family=Shrikhand&family=Rampart+One&family=Rubik+Glitch&family=Archivo+Black&display=swap';
  document.head.appendChild(l);
}
// Portada estilo revista: cabecera, franja, sticker, titular y código de barras (todo dibujado con CSS).
function revMag(title, sub, cover, st, date, mh0, small) {
  const im = revPic(cover), sm = small ? revThumb(im) : im, d = revDate(date || Date.now(), { month: 'short', year: 'numeric' }).toUpperCase();
  return `<header class="rv-mag${small ? ' sm' : ''}">${im ? `<img class="rv-mag-img" src="${esc(sm)}"${sm !== im ? ` data-full="${esc(im)}"` : ''} alt="" loading="lazy" decoding="async">` : ''}<div class="rv-mag-strip"><span>edición especial · ${esc(d)} · edición especial · ${esc(d)}</span></div><div class="rv-mag-mast">${esc(st.mh || mh0 || 'Radio de ruta')}</div>${st.stk ? `<span class="rv-stk"><span>${esc(st.stk)}</span></span>` : ''}<div class="rv-mag-lines"><h2>${esc(title)}</h2>${sub ? `<p class="rv-sub">${esc(sub)}</p>` : ''}</div><i class="rv-code" aria-hidden="true"></i></header>`;
}
function revHeader(title, sub, cover, st, by, date, mh0) {
  if (st.cv === 'm') return revMag(title, sub, cover, st, date, mh0) + (by || '');
  const im = revPic(cover), tt = `<h2>${esc(title)}</h2>${sub ? `<p class="rv-sub">${esc(sub)}</p>` : ''}`;
  if (st.cv === 'f' && im) return `<header class="rv-hdr"><img class="rv-cov" src="${esc(im)}" alt=""><div class="rv-ov">${tt}</div></header>${by || ''}`;
  return `${im ? `<img class="rv-cov" src="${esc(im)}" alt="">` : ''}${tt}${by || ''}`;
}
// Selector de estética reutilizable (artículos, perfiles y artistas). t = 'st' | 'pst' | 'ast'.
function revStyleUI(t, full) {
  const st = rev[t], on = (k, v) => st[k] === v ? ' on' : '';
  const sw = (k, v, lab, col) => `<button type="button" class="rv-sw${on(k, v)}" data-act="st" data-t="${t}" data-k="${k}" data-v="${v}"><i style="background:${col}"></i>${lab}</button>`;
  const opt = (k, v, lab) => `<button type="button" class="rv-sw${on(k, v)}" data-act="st" data-t="${t}" data-k="${k}" data-v="${v}">${lab}</button>`;
  const tg = (k, v, lab) => `<button type="button" class="rv-sw${st[k] === v ? ' on' : ''}" data-act="st" data-t="${t}" data-k="${k}" data-v="${st[k] === v ? REV_DEF[k] : v}">${lab}</button>`;
  const ths = Object.keys(REV_TH), basic = ths.slice(0, 6), mag = ths.slice(6);
  let h = `<p class="rv-sl">Estéticas de revista</p><div class="rv-sws">${mag.map(k => sw('th', k, REV_TH[k], REV_THC[k])).join('')}</div>
<p class="rv-sl">Básicas</p><div class="rv-sws">${basic.map(k => sw('th', k, REV_TH[k], REV_THC[k])).join('')}</div>
<p class="rv-sl">Tipografía</p><div class="rv-sws">${Object.keys(REV_FT).map(k => `<button type="button" class="rv-sw rv-ft${on('ft', k)}" data-act="st" data-t="${t}" data-k="ft" data-v="${k}" data-ft="${k}">${REV_FT[k]}</button>`).join('')}</div>
<p class="rv-sl">Color de acento</p><div class="rv-sws">${Object.keys(REV_AC).map(k => sw('ac', k, k[0].toUpperCase() + k.slice(1), REV_AC[k])).join('')}</div>`;
  if (full) h += `<p class="rv-sl">Portada</p><div class="rv-sws">${opt('cv', 'a', 'Imagen arriba')}${opt('cv', 'f', 'Imagen de fondo')}${opt('cv', 'm', 'Portada de revista')}</div>
${st.cv === 'm' ? `<div class="rv-two"><label>Nombre de la revista<input id="rvMh" data-t="${t}" maxlength="22" placeholder="Radio de ruta" value="${esc(st.mh)}" autocomplete="off"></label><label>Sticker<input id="rvStk" data-t="${t}" maxlength="24" placeholder="¡Nuevo!" value="${esc(st.stk)}" autocomplete="off"></label></div>` : ''}
<p class="rv-sl">Detalles</p><div class="rv-sws">${tg('al', 'c', 'Títulos centrados')}${tg('dc', 1, 'Letra capital')}${tg('sz', 'g', 'Título grande')}</div>`;
  return h;
}

/* --- Estado y navegación --- */
const rev = { sec: 'art', view: 'feed', tab: 'all', posts: [], off: 0, more: false, t: 0, extra: null, post: null, comments: [], likes: new Set(), follows: new Set(), socialUid: null, me: null, req: 0,
  tracks: [], cover: '', editId: null, ids: [], st: {}, hadStyle: false, pst: {}, pav: '', ast: {}, acover: '', stack: [], prof: null, profTab: 'posts',
  artists: null, aT: 0, aQ: '', artist: null, aPosters: [], aPosts: [], posters: [], pOff: 0, pMore: false, pFilter: 'all', pQ: '', poster: null, found: [], cq: '', deep: null, v1: false, v3: false, recs: new Set(), curators: new Set(), curList: [], vRows: [], vTab: 'now', vCycle: '', vItem: null, vComs: [], unread: 0, polls: [], events: [], agMode: 'list', reading: false, replyTo: null, aManagers: [] };
const REV_CARD = () => `id,title,subtitle,cover,style${rev.v3 ? '' : ',review'}${rev.v5off ? '' : ',tags,views,status,publish_at'},created_at,author_id,profiles!author_id(name${rev.v1 ? '' : ',avatar'}),likes(count),comments(count)`;
const revBody = () => $('revBody');
const revIsAdmin = () => !!(rev.me && rev.me.role === 'admin');
const revIsEditor = () => !!(rev.me && (rev.me.role === 'editor' || rev.me.role === 'admin'));
const REV_TRANS = new Set(['edit', 'editprof', 'editartist', 'commons', 'rules', 'editedition', 'compare', 'editvault', 'editclub']);
const revBanned = () => !!(rev.me && rev.me.banned);
function revPush() {
  if (REV_TRANS.has(rev.view)) return;
  const v = rev.view, id = v === 'edition' ? rev.edition && rev.edition.id : v === 'collection' ? rev.col && rev.col.id : v === 'post' ? rev.post && rev.post.id : v === 'profile' ? rev.prof && rev.prof.id : v === 'artist' ? rev.artist && rev.artist.id : v === 'poster' ? rev.poster && rev.poster.id : v === 'vitem' ? rev.vItem && rev.vItem.id : v === 'club' ? rev.club && rev.club.id : null;
  rev.stack.push({ sec: rev.sec, view: v, tab: rev.tab, id }); if (rev.stack.length > 30) rev.stack.shift();
}
function revBack() {
  const s = rev.stack.pop();
  if (!s) return revSection(rev.sec);
  rev.sec = s.sec; rev.tab = s.tab;
  if (s.view === 'post') return revOpen(s.id, true);
  if (s.view === 'profile') return revProfile(s.id, true);
  if (s.view === 'artist') return revArtist(s.id, true);
  if (s.view === 'poster') return revPosterOpen(s.id, true);
  if (s.view === 'artists') return revArtists();
  if (s.view === 'posters') return revPosters(false);
  if (s.view === 'admin') return revAdmin(rev.admTab || 'reports');
  if (s.view === 'agenda') return revAgenda();
  if (s.view === 'vitem') return revVaultOpen(s.id, true);
  if (s.view === 'vault') return revVault();
  if (s.view === 'club') return clubView(s.id, true);
  if (s.view === 'notifs') return revNotifs(true);
  if (s.view === 'search') return revSearchView(true);
  if (s.view === 'edition') return revEdition(s.id, true);
  if (s.view === 'collection') return revCollection(s.id, true);
  rev.view = 'feed'; revBar(); return rev.posts.length ? revDrawFeed() : revFeed(true);
}
function revSection(sec) {
  rev.sec = sec; rev.stack = [];
  if (sec === 'artists') return revArtists();
  if (sec === 'posters') return revPosters(true);
  if (sec === 'admin') return revAdmin(rev.admTab || 'reports');
  if (sec === 'agenda') return revAgenda();
  if (sec === 'vault') return revVault('now');
  if (sec === 'club') return clubView(null, true);
  rev.view = 'feed'; revBar(); return revFeed(true);
}
const revHead = (back, title, extra = '') => `<div class="rv-head"><button type="button" class="alt" data-act="back">‹ ${back}</button>${title ? `<h3>${esc(title)}</h3>` : ''}${extra}</div>`;
function revBar() {
  const me = revMe(), bar = $('revBar'), fab = $('revFab');
  bar.hidden = false;
  const sec = (v, n) => `<button type="button" class="rv-sec${rev.sec === v ? ' on' : ''}" data-act="sec" data-v="${v}">${n}</button>`;
  const tab = (v, n) => `<button type="button" class="rv-tab${rev.tab === v ? ' on' : ''}" data-act="tab" data-v="${v}">${n}</button>`;
  const srch = `<button type="button" class="rv-bell" data-act="search" aria-label="Buscar">${ICO('search')}</button>`;
  const acctHtml = () => me ? `<span class="rv-meb">${srch}<button type="button" class="rv-bell" data-act="notifs" aria-label="Notificaciones">${ICO('bell')}<b${rev.unread ? '' : ' hidden'}>${rev.unread > 9 ? '9+' : rev.unread}</b></button><button type="button" class="rv-me" data-act="acct" aria-label="Mi perfil">${revAv(rev.me || { name: me.name }, 34)}</button></span>` : `<span class="rv-meb">${srch}<button type="button" class="alt" data-act="acct">Entrar</button></span>`;
  $('revAcct').innerHTML = acctHtml();
  bar.innerHTML = nvDock() +
    (revBanned() ? '<p class="rv-warn">Tu cuenta está suspendida: puedes leer, pero no publicar.</p>' : '') +
    (rev.sec === 'art' && rev.view === 'feed' ? `<div class="rv-tabs nv-tabs">${tab('all', 'Descubrir')}${tab('follow', 'Siguiendo')}${tab('polls', 'Debates')}${tab('editions', 'Ediciones')}${tab('mine', 'Mis artículos')}</div>` : '');
  const showFab = !REV_TRANS.has(rev.view) && !['search', 'edition', 'collection', 'post', 'profile'].includes(rev.view) && rev.sec !== 'admin' && rev.sec !== 'more' && !revBanned() && (rev.sec !== 'artists' || revIsEditor()) && (rev.sec !== 'vault' || (rev.view === 'vault' && vaultCan('create'))) && rev.sec !== 'club' && (rev.sec !== 'art' || rev.tab !== 'editions' || revIsAdmin());
  fab.hidden = !showFab;
  fab.setAttribute('aria-label', rev.sec === 'vault' ? 'Programar concierto' : rev.sec === 'agenda' ? 'Nuevo concierto' : rev.sec === 'art' ? (rev.tab === 'polls' ? 'Nuevo debate' : 'Escribir artículo') : rev.sec === 'posters' ? 'Añadir póster' : 'Nuevo artista');
}
function renderRevista() {
  const hint = $('revHint'), box = revBody();
  if (!revOk()) { $('revBar').hidden = true; $('revFab').hidden = true; $('revAcct').innerHTML = ''; box.innerHTML = ''; hint.textContent = revMine() ? 'La revista aún no está configurada. Ve a Ajustes > Revista y pega la URL y la clave de tu proyecto de Supabase.' : 'Esta copia de Radio de ruta no usa la revista original: cada página tiene su propia revista. Para crear la tuya, abre un proyecto gratis en supabase.com, ejecuta revista_supabase_completo.sql y pega tu URL y tu clave pública en Ajustes > Revista (los pasos están en el README).'; return; }
  hint.textContent = '';
  revFonts(); revBar();
  if (!navigator.onLine) { if (!box.innerHTML) box.innerHTML = svOfflineHtml(); return; }
  if (rev.socialUid === null && revMe()) revSocial().then(revBar);
  if (revMe()) revNotifCount();
  if (window.revRecovery) return revRecover();
  if (rev.deep) { const d = rev.deep; rev.deep = null; return d.k === 'rv' ? revOpen(d.id) : d.k === 'rvb' ? revVaultOpen(d.id) : d.k === 'rvp' ? revPosterOpen(d.id) : revArtist(d.id); }
  if (rev.sec === 'art' && rev.view === 'feed' && (!rev.posts.length || Date.now() - rev.t > 60000)) revFeed(true);
  else if (rev.sec === 'artists' && rev.view === 'artists' && Date.now() - rev.aT > 60000) revArtists();
  else if (rev.sec === 'posters' && rev.view === 'posters' && !rev.posters.length) revPosters(true);
  else if (rev.sec === 'admin' && rev.view === 'admin') revAdmin(rev.admTab || 'reports');
  else if (rev.sec === 'agenda' && rev.view === 'agenda') revAgenda();
  else if (rev.sec === 'vault' && rev.view === 'vault') revVault();
  else if (!box.innerHTML) revSection(rev.sec);
}
async function revSocial(force) {
  const me = revMe(); if (!me) { rev.likes = new Set(); rev.follows = new Set(); rev.socialUid = null; rev.me = null; return; }
  if (!force && rev.socialUid === me.uid) return;
  try {
    const [l, f] = await Promise.all([revFetch(`/rest/v1/likes?select=post_id&user_id=eq.${me.uid}`), revFetch(`/rest/v1/follows?select=author_id&follower_id=eq.${me.uid}`)]);
    rev.likes = new Set(l.map(x => x.post_id)); rev.follows = new Set(f.map(x => x.author_id)); rev.socialUid = me.uid;
  } catch {}
  try { rev.recs = new Set((await revFetch(`/rest/v1/recommends?select=post_id&user_id=eq.${me.uid}`)).map(x => x.post_id)); rev.v3 = false; } catch { rev.recs = new Set(); }
  try { const p = await revFetch(`/rest/v1/profiles?select=*&id=eq.${me.uid}`); rev.me = p[0] || { id: me.uid, name: me.name, role: 'user' }; rev.v1 = false; }
  catch (e) { rev.me = { id: me.uid, name: me.name, role: 'user' }; if (/does not exist|schema cache|Could not find/i.test(e.raw || '')) rev.v1 = true; }
}
/* --- Feed de artículos --- */
async function revExtras() {
  const me = revMe(), out = { stories: [], archive: null, people: [], today: [] };
  if (me) await revSocial();
  const old = encodeURIComponent(new Date(Date.now() - 30 * 864e5).toISOString());
  const jobs = [
    revFetch(`/rest/v1/posts?select=${REV_CARD()}&created_at=lt.${old}${REV_PUB()}&order=created_at.asc&limit=20`).then(a => { if (a.length) out.archive = a[Math.floor(Math.random() * a.length)]; }),
    revFetch(`/rest/v1/profiles?select=id,name${rev.v1 ? '' : ',avatar,bio'}&order=created_at.desc&limit=24`).then(a => { out.people = a.filter(p => (!me || p.id !== me.uid) && !rev.follows.has(p.id)).slice(0, 8); })
  ];
  jobs.push(revToday().then(e => { out.today = e; }), nvHomeExtras(out));
  const ids = [...rev.follows].filter(i => UUID.test(i)).slice(0, 30);
  if (ids.length) jobs.push(revFetch(`/rest/v1/profiles?select=id,name${rev.v1 ? '' : ',avatar'}&id=in.(${ids.join(',')})`).then(a => { out.stories = a; }));
  await Promise.allSettled(jobs);
  return out;
}
async function revFeed(reset) {
  if (rev.tab === 'polls') return revPolls();
  if (rev.tab === 'editions') return revEditions();
  const my = ++rev.req, box = revBody(), me = revMe();
  rev.view = 'feed';
  if (reset) { rev.off = 0; rev.posts = []; rev.more = false; rev.extra = null; box.innerHTML = '<p class="hint">Cargando…</p>'; }
  try {
    let q = `/rest/v1/posts?select=${REV_CARD()}&order=created_at.desc&limit=${REV_PAGE}&offset=${rev.off}${rev.tab === 'mine' ? '' : REV_PUB()}${rev.tagF && !rev.v5off ? `&tags=cs.${encodeURIComponent('{' + rev.tagF + '}')}` : ''}`;
    if (rev.tab === 'mine') { if (!me) { box.innerHTML = '<p class="hint">Entra a tu cuenta para ver tus artículos.</p>'; return; } q += `&author_id=eq.${me.uid}`; }
    else if (rev.tab === 'follow') {
      if (!me) { box.innerHTML = '<p class="hint">Entra a tu cuenta para ver a los autores que sigues.</p>'; return; }
      await revSocial(true); const ids = [...rev.follows].filter(i => UUID.test(i));
      if (!ids.length) { box.innerHTML = '<p class="hint">Aún no sigues a nadie. Abre el perfil de un autor y toca «Seguir».</p>'; return; }
      q += `&author_id=in.(${ids.join(',')})`;
    }
    let [data, extra] = await Promise.all([revFetch(q), reset && rev.tab === 'all' ? revExtras() : Promise.resolve(rev.extra)]);
    if (rev.tab === 'follow' && reset && !rev.v3) data = await revMergeRecs(data);
    if (my !== rev.req) return;
    rev.extra = extra; rev.posts = rev.posts.concat(data); rev.off += data.length; rev.more = data.length === REV_PAGE; rev.t = Date.now();
    revDrawFeed();
  } catch (e) {
    if (!rev.v5off && /status|publish_at|tags|views|gallery/i.test(e.raw || '')) { rev.v5off = true; return revFeed(true); } // base sin v5: sigue funcionando
    if (!rev.v3 && /review|recommends|does not exist|schema cache/i.test(e.raw || '')) { rev.v3 = true; return revFeed(true); } // base sin v4: sigue funcionando
    if (!rev.v1 && /avatar|does not exist|schema cache/i.test(e.raw || '')) { rev.v1 = true; return revFeed(true); } // base de datos sin v2: sigue funcionando
    if (my === rev.req) box.innerHTML = `<p class="hint">${esc(e.message)}</p><button type="button" class="alt" data-act="retry">Reintentar</button>`;
  }
}
function revCard(p, hero, arch) {
  const st = revSt(p.style), im = revPic(p.cover);
  const me = revMe(), mine = me && me.uid === p.author_id;
  const au = `<div class="nv-cau"><span class="nv-cwho" data-act="profile" data-id="${esc(p.author_id)}">${revAv(p.profiles, 32)}<span><b>${esc(revName(p))}</b><small>${revAgo(p.created_at)}</small></span></span></div>`;
  const acts = `<div class="nv-cacts"><span>${ICO('heart')} ${revCnt(p.likes)}</span><span>${ICO('chat')} ${revCnt(p.comments)}</span>${mine && p.views ? `<span>${p.views} lecturas</span>` : ''}</div>`;
  const meta = `${revState(p)}${revTagChips(p)}${acts}`;
  const lab = arch ? '<p class="rv-lab">Del archivo</p>' : p._rec ? `<p class="rv-lab">Recomendado por ${esc(p._rec.name)}${p._rec.note ? ': «' + esc(p._rec.note) + '»' : ''}</p>` : '';
  const rv = revReview(p.review), chip = rv ? `<span class="rv-scorechip">★ ${rv.score}/10 · ${esc(rv.album)}</span>` : '';
  const a = `class="rv-card nv-card rv-st${st.cv === 'm' ? ' mag' : ''}${hero ? ' hero' : ''}" ${revAttrs(st)} data-act="open" data-id="${esc(p.id)}" tabindex="0"`;
  if (st.cv === 'm') return `<article ${a}>${lab}<div class="rv-in nv-ctop">${au}</div>${revMag(p.title, p.subtitle, p.cover, st, p.created_at, '', true)}<div class="rv-in">${chip}${meta}</div></article>`;
  return `<article ${a}>${lab}<div class="rv-in nv-ctop">${au}</div>${im ? `<div class="nv-cimg">${revImgTag(im)}</div>` : ''}<div class="rv-in">${chip}<h3>${esc(p.title)}</h3>${p.subtitle ? `<p class="rv-sub">${esc(p.subtitle)}</p>` : ''}${meta}</div></article>`;
}
const revGrid = list => `<div class="rv-grid">${list.map((p, i) => revCard(p, false)).join('')}</div>`;
const revFollowBtn = id => { const me = revMe(); if (me && me.uid === id) return ''; const on = rev.follows.has(id); return `<button type="button" class="rv-fol${on ? ' on' : ''}" data-act="follow" data-id="${esc(id)}">${on ? 'Siguiendo' : 'Seguir'}</button>`; };
function revDrawFeed() {
  const box = revBody(), x = rev.tab === 'all' ? rev.extra : null;
  let h = '';
  if (rev.tagF) h += `<p class="rv-tagf">Artículos con <b>#${esc(rev.tagF)}</b> <button type="button" class="rv-x" data-act="tag" data-v="">quitar filtro</button></p>`;
  if (rev.tab === 'all' && !rev.tagF) h += nvHello();
  if (x && !rev.tagF) h += nvVaultBanner(x.vday) + nvClubBanner(x.club) + revTodayBox(x.today);
  if (x && x.stories.length) h += `<div class="rv-stories" aria-label="Autores que sigues">${x.stories.map(p => `<button type="button" class="rv-story" data-act="profile" data-id="${esc(p.id)}">${revAv(p, 58)}<span>${esc(p.name)}</span></button>`).join('')}</div>`;
  const list = rev.posts.slice(); let arch = null;
  if (x && x.archive && !list.some(p => p.id === x.archive.id) && list.length >= 3) { arch = x.archive; list.splice(3, 0, arch); }
  if (!list.length) h += '<p class="hint">Todavía no hay artículos aquí. ¡Sé la primera persona en escribir uno con el botón +!</p>';
  else { const cards = list.map((p, i) => revCard(p, i === 0, p === arch)), row = x && !rev.tagF ? nvPosterRow(x.posters) : ''; h += '<div class="rv-grid">' + cards.slice(0, 4).join('') + '</div>' + row + (cards.length > 4 ? '<div class="rv-grid">' + cards.slice(4).join('') + '</div>' : ''); }
  if (rev.more) h += '<button type="button" class="alt rv-more" data-act="more">Cargar más</button>';
  if (x && x.people.length) h += `<div class="nv-rowh"><h4>Personas para seguir</h4></div><div class="rv-people nv-hscroll">${x.people.map(p => `<div class="rv-pc">${revAv(p, 64)}<b>${esc(p.name)}</b><small>${esc((p.bio || '').slice(0, 70))}</small><div class="rv-pcb"><button type="button" class="alt" data-act="profile" data-id="${esc(p.id)}">Ver</button>${revFollowBtn(p.id)}</div></div>`).join('')}</div>`;
  box.innerHTML = h;
}
/* --- Artículo --- */
async function revOpen(id, noPush) {
  if (!UUID.test(id)) return;
  if (!noPush) revPush();
  const box = revBody(); rev.sec = 'art'; rev.view = 'post'; const my = ++rev.req; box.innerHTML = '<p class="hint">Cargando…</p>'; revBar();
  try {
    const [p, cm] = await Promise.all([revFetch(`/rest/v1/posts?select=*,profiles!author_id(name${rev.v1 ? '' : ',avatar'}),likes(count)${rev.v3 ? '' : ',recommends(count)'}&id=eq.${id}`).catch(e => { if (rev.v3) throw e; rev.v3 = true; return revFetch(`/rest/v1/posts?select=*,profiles!author_id(name${rev.v1 ? '' : ',avatar'}),likes(count)&id=eq.${id}`); }), revFetch(`/rest/v1/comments?select=*,profiles!author_id(name${rev.v1 ? '' : ',avatar'})&post_id=eq.${id}&order=created_at.asc`), revSocial()]);
    if (!rev.post || rev.post.id !== id) { rev.replyTo = null; if (rev.reading) revStopRead(); }
    if (my !== rev.req) return;
    if (!p[0]) { box.innerHTML = revHead('Volver', '') + '<p class="hint">Ese artículo ya no existe.</p>'; return; }
    rev.post = p[0]; rev.comments = cm; rev.shown = null; await revDrawPost(); revCountView(rev.post);
  } catch (e) { if (my === rev.req) box.innerHTML = revHead('Volver', '') + `<p class="hint">${esc(e.message)}</p>`; }
}
async function revDrawPost() {
  const p = rev.post, me = revMe(), box = revBody(), mine = me && me.uid === p.author_id, adm = revIsAdmin();
  let lib = []; try { lib = await libAll(); } catch {}
  const map = new Map(); lib.forEach(r => { const k = revNk(r.name) + '|' + revNk(r.artist); if (!map.has(k)) map.set(k, r.id); });
  rev.ids = [];
  const tr = (Array.isArray(p.tracks) ? p.tracks : []).slice(0, 12).map(t => {
    const id = map.get(revNk(t.name) + '|' + revNk(t.artist)), has = id !== undefined; if (has) rev.ids.push(id);
    return `<div class="rv-tr">${ICO('music')}<div><b>${esc(t.name || '')}</b><span>${esc(t.artist || '')}${t.album ? ' · ' + esc(t.album) : ''}</span></div>${has ? `<button type="button" class="alt" data-act="play" data-i="${rev.ids.length - 1}">Reproducir</button>` : '<em>No está en tu biblioteca</em>'}</div>`;
  }).join('');
  const st = revSt(p.style), liked = rev.likes.has(p.id), recd = rev.recs.has(p.id), rv = revReview(p.review);
  const tops = rev.comments.filter(c => !c.parent_id || !rev.comments.some(x => x.id === c.parent_id)), kids = id => rev.comments.filter(c => c.parent_id === id);
  const cm = (c, top) => `<div class="rv-c${top ? '' : ' rv-reply'}"><p><a href="#" data-act="profile" data-id="${esc(c.author_id)}">${revAv(c.profiles, 22)} <b>${esc(revName(c))}</b></a> <small>${revAgo(c.created_at)}</small>${me && (me.uid === c.author_id || adm) ? ` <button type="button" class="rv-x" data-act="delc" data-id="${esc(c.id)}">Borrar</button>` : ''}${!me || me.uid !== c.author_id ? ` <button type="button" class="rv-x" data-act="report" data-type="comment" data-id="${esc(c.id)}">Denunciar</button>` : ''}${rev.v3 ? '' : ` <button type="button" class="rv-x" data-act="reply" data-id="${esc(top ? c.id : c.parent_id)}" data-name="${esc(revName(c))}">Responder</button>`}</p><p>${esc(c.body).replace(/\n/g, '<br>')}</p></div>`;
  const by = `<div class="rv-by nv-by"><a href="#" data-act="profile" data-id="${esc(p.author_id)}">${revAv(p.profiles, 42)}</a><span><a href="#" data-act="profile" data-id="${esc(p.author_id)}"><b>${esc(revName(p))}</b></a><small>${revDate(p.created_at)} · ${Math.max(1, Math.round(String(p.body || '').split(/\s+/).length / 200))} min de lectura</small></span>${revFollowBtn(p.author_id)}</div>`;
  box.innerHTML = revHead('Volver', '', `<span class="rv-sp"></span>${mine ? '<button type="button" class="alt" data-act="edit">Editar</button>' : ''}${mine || adm ? '<button type="button" class="alt" data-act="del">Eliminar</button>' : ''}`) +
`<article class="rv-art rv-st" ${revAttrs(st)}>${revHeader(p.title, p.subtitle, p.cover, st, by, p.created_at)}${rv ? revReviewBox(rv) : ''}
<div class="rv-pbtns rv-listen"><button type="button" class="rv-like" data-act="read">${ICO(rev.reading ? 'stop' : 'mic')} ${rev.reading ? 'Detener lectura' : 'Escuchar artículo'}</button>${rev.ids.length ? `<button type="button" class="rv-like" data-act="playlist">${ICO('play')} Escuchar esta lista (${rev.ids.length})</button>` : ''}</div>
<div class="rv-txt" data-dc="${st.dc}">${revFmt(p.body)}</div>${revGallery(p.gallery)}${revTagChips(p)}${mine ? `<p class="rv-m">${revState(p)} ${p.views || 0} lecturas</p>` : ''}${tr ? `<h4 class="rv-h">Canciones del artículo</h4>${tr}` : ''}
<div class="rv-acts"><button type="button" class="rv-like${liked ? ' on' : ''}" data-act="like">${ICO('heart')} ${liked ? 'Te gusta' : 'Me gusta'} · ${revCnt(p.likes)}</button>${!mine && !rev.v3 ? `<button type="button" class="rv-like${recd ? ' on' : ''}" data-act="rec">${ICO('repeat')} ${recd ? 'Recomendado' : 'Recomendar'} · ${revCnt(p.recommends)}</button>` : ''}<button type="button" class="rv-like" data-act="sharepost">${ICO('share')} Compartir</button><button type="button" class="rv-like" data-act="story" data-v="post">${ICO('image')} Imagen para historias</button>${mine ? '' : `<button type="button" class="rv-x" data-act="report" data-type="post" data-id="${esc(p.id)}">Denunciar</button>`}</div>
<div class="nv-pill" role="toolbar" aria-label="Acciones del artículo"><button type="button" class="${liked ? 'on' : ''}" data-act="like" aria-label="Me gusta" aria-pressed="${liked}">${ICO('heart')} ${revCnt(p.likes)}</button><button type="button" data-act="nvgocom" aria-label="Comentarios">${ICO('chat')} ${rev.comments.length}</button>${!mine && !rev.v3 ? `<button type="button" class="${recd ? 'on' : ''}" data-act="rec" aria-label="Recomendar" aria-pressed="${recd}">${ICO('repeat')} ${revCnt(p.recommends)}</button>` : ''}<button type="button" data-act="sharepost" aria-label="Compartir">${ICO('share')}</button></div></article>
<section class="rv-com"><h4 class="rv-h">Comentarios (${rev.comments.length})</h4>${tops.map(c => cm(c, true) + kids(c.id).map(k => cm(k, false)).join('')).join('') || '<p class="hint">Sé la primera persona en comentar.</p>'}
${rev.replyTo ? `<p class="rv-replyto">Respondiendo a <b>${esc(rev.replyTo.name)}</b> <button type="button" class="rv-x" data-act="noreply">cancelar</button></p>` : ''}<textarea id="rvCom" rows="3" maxlength="1000" placeholder="Escribe un comentario…"></textarea><button type="button" data-act="comment">Comentar</button></section>`;
  try { dzApply(box.querySelector('article.rv-art'), p.design, p.cover); } catch {}
  if (rev.shown !== p.id) { rev.shown = p.id; box.scrollIntoView({ block: 'start' }); }
}
/* --- Cuenta --- */
async function revNeed() {
  if (revMe()) return true;
  toast('Entra o crea tu cuenta para hacer esto.');
  await revAcct(); return !!revMe();
}
async function revAcct() {
  const me = revMe();
  if (me) return revProfile(me.uid);
  const mode = await uiDialog({ title: 'Revista de Radio de ruta', text: 'Con una cuenta puedes escribir, comentar, dar me gusta, seguir autores y subir pósters.', options: [{ label: 'Entrar', value: 'login' }, { label: 'Crear cuenta', value: 'signup' }, { label: 'Olvidé mi contraseña', value: 'forgot' }], cancel: 'Ahora no' });
  if (!mode) return;
  if (mode === 'forgot') return revForgot();
  const f = [{ key: 'email', label: 'Correo', type: 'email' }, { key: 'pass', label: 'Contraseña (mínimo 6)', type: 'password' }];
  let inv = false;
  if (mode === 'signup' && !(await uiConfirm('Normas de la comunidad', revRulesText(), 'Acepto', false))) return;
  if (mode === 'signup') {
    f.unshift({ key: 'name', label: 'Tu nombre público', hint: 'Así te verán en tus artículos y comentarios.' });
    try { inv = (await revFetch('/rest/v1/rpc/rr_invite_only', { method: 'POST', body: {} })) === true; } catch {}
    if (inv) f.push({ key: 'invite', label: 'Código de invitación', hint: 'Por ahora la revista es solo con invitación. Pídele el código al administrador.' });
  }
  const v = await uiDialog({ title: mode === 'signup' ? 'Crear cuenta' : 'Entrar', fields: f, ok: mode === 'signup' ? 'Crear cuenta' : 'Entrar' });
  if (!v) return;
  const email = (v.email || '').trim(), pass = v.pass || '', name = (v.name || '').trim().slice(0, 40);
  if (!/^\S+@\S+\.\S+$/.test(email) || pass.length < 6) return toast('Revisa el correo y la contraseña (mínimo 6).', true);
  if (mode === 'signup' && name.length < 2) return toast('Escribe tu nombre (mínimo 2 letras).', true);
  if (inv && !(v.invite || '').trim()) return toast('Escribe el código de invitación.', true);
  toast(mode === 'signup' ? 'Creando tu cuenta…' : 'Entrando…', false, 15000);
  try {
    const ok = await revAuth(mode, email, pass, name, inv ? (v.invite || '').trim() : '');
    if (ok) { toast('¡Listo! Ya estás dentro.'); await revSocial(true); if (rev.me && rev.me.name) revSaveSes({ ...revMe(), name: rev.me.name }); revBar(); if (rev.view === 'feed') revFeed(true); }
    else toast('Cuenta creada. Te llegó un correo: toca «Confirm email address» y volverás aquí ya con tu cuenta abierta. Revisa también spam.', false, 10000);
  } catch (e) {
    if (/Confirma tu correo/.test(e.message)) return revResend(email);
    toast(e.message, true, 6000);
  }
}
function revLogout() { revSaveSes(null); rev.likes = new Set(); rev.follows = new Set(); rev.socialUid = null; rev.me = null; toast('Sesión cerrada.'); revSection('art'); }
/* --- Editor de artículos --- */
const revDraft = () => { try { return JSON.parse(localStorage.getItem('rr_rev_draft') || 'null'); } catch { return null; } };
function revEditor(p) {
  revPush(); ++rev.req; rev.view = 'edit'; rev.editId = p ? p.id : null;
  const d = p ? null : revDraft();
  rev.tracks = p ? (Array.isArray(p.tracks) ? p.tracks.slice() : []) : ((d && d.tracks) || []);
  rev.cover = p ? revPic(p.cover) : ((d && revImg(d.cover)) || ''); rev.editCover = p ? p.cover || null : null;
  rev.st = revSt(p ? p.style : d && d.st); rev.hadStyle = !!(p && p.style && Object.keys(p.style).length);
  revBar();
  revBody().innerHTML = revHead('Cancelar', p ? 'Editar artículo' : 'Nuevo artículo') + `
<div class="rv-form"><label for="rvTitle">Título</label><input id="rvTitle" maxlength="150" autocomplete="off" value="${esc(p ? p.title : (d && d.title) || '')}">
${rev.v5off ? '' : `<div class="rv-aiw"><span>Ayuda de tu IA:</span><button type="button" class="rv-x" data-act="aiw" data-v="fix">corregir ortografía</button><button type="button" class="rv-x" data-act="aiw" data-v="titles">sugerir títulos</button><button type="button" class="rv-x" data-act="aiw" data-v="sum">resumir en subtítulo</button></div>`}
<label for="rvSub">Subtítulo (opcional)</label><input id="rvSub" maxlength="200" autocomplete="off" value="${esc(p ? p.subtitle || '' : (d && d.subtitle) || '')}">
<label>Imagen de portada</label><div id="rvCovPrev"></div><div class="row"><button type="button" class="alt" data-act="pickcover">Elegir imagen</button><button type="button" class="alt" data-act="nocover">Quitar</button></div><input type="file" id="rvCovFile" accept="image/*" hidden>
<label>Estética del artículo</label><div id="rvStyle"></div><div id="rvPrev" class="rv-art rv-st"></div>
<label>Diseño avanzado: fondo, difuminados, transparencia, letra y collage</label><div id="rvDzUI"></div>
${rev.v3 ? '' : `<div id="rvReviewUI">${revReviewUI(p ? revReview(p.review) : d && revReview(d.review))}</div>`}
<label for="rvText">Texto</label><div class="row" style="margin-bottom:6px"><button type="button" class="alt" data-act="fmt" data-f="b"><b>N</b></button><button type="button" class="alt" data-act="fmt" data-f="i"><i>K</i></button><button type="button" class="alt" data-act="fmt" data-f="h">Título</button><button type="button" class="alt" data-act="fmt" data-f="q">Cita</button><button type="button" class="alt" data-act="fmt" data-f="l">Lista</button></div>
<textarea id="rvText" rows="14" maxlength="20000" placeholder="Escribe tu reseña, tu lista o tu historia musical…">${esc(p ? p.body : (d && d.body) || '')}</textarea>
<p class="hint">Deja una línea en blanco entre párrafos. **negrita** · *cursiva* · «## » título · «> » cita · «- » lista.</p>
${rev.v5off ? '' : `<label>Galería de fotos (hasta 8)</label><div id="rvGalPrev" class="rv-gal"></div><div class="row"><button type="button" class="alt" data-act="pickgal">Añadir fotos</button></div><input type="file" id="rvGalFile" accept="image/*" multiple hidden>
<label for="rvTags">Etiquetas (hasta 5)</label><input id="rvTags" maxlength="140" autocomplete="off" placeholder="grunge, en vivo, reseña" value="${esc(((p && p.tags) || (d && d.tags) || []).join(', '))}">
<label for="rvWhen">Programar publicación (opcional)</label><input id="rvWhen" type="datetime-local" value="${p && p.publish_at && new Date(p.publish_at) > new Date() ? esc(new Date(new Date(p.publish_at).getTime() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 16)) : ''}">`}
<label for="rvFind">Canciones de tu biblioteca</label><input id="rvFind" autocomplete="off" placeholder="Busca por canción o artista"><div id="rvRes"></div><div id="rvTrk"></div>
<div class="row" style="margin-top:14px">${rev.v5off ? '' : '<button type="button" class="alt" data-act="publish" data-v="draft">Guardar borrador</button>'}<button type="button" data-act="publish" data-v="pub">${p && p.status !== 'draft' ? 'Guardar cambios' : 'Publicar'}</button></div></div>`;
  rev.gal = (p ? (Array.isArray(p.gallery) ? p.gallery.slice() : []) : ((d && d.gallery) || [])).filter(u => revPic(u)); rev.editGal = p && Array.isArray(p.gallery) ? p.gallery.slice() : [];
  if (!rev.v5off) revDrawGal();
  dzInit(p, d); revDrawCov(); revDrawTrk(); revStChanged();
}
const revDrawCov = () => { $('rvCovPrev').innerHTML = rev.cover ? `<img class="rv-cov" src="${esc(rev.cover)}" alt="" style="max-height:140px">` : '<p class="hint">Sin imagen.</p>'; revStChanged(true); };
const revDrawTrk = () => { $('rvTrk').innerHTML = rev.tracks.map((t, i) => `<div class="rv-tr">${ICO('music')}<div><b>${esc(t.name)}</b><span>${esc(t.artist || '')}</span></div><button type="button" class="alt" data-act="deltr" data-i="${i}">Quitar</button></div>`).join(''); };
function revPaint(el, st) { ['th', 'ft', 'ac', 'al', 'dc', 'sz', 'cv'].forEach(k => el.setAttribute('data-' + k, st[k])); el.setAttribute('data-lt', REV_LIGHT.has(st.th) ? 1 : 0); }
// Redibuja el selector y la vista previa del formulario abierto.
function revStChanged(onlyPrev) {
  const v = rev.view, t = v === 'edit' ? 'st' : v === 'editprof' ? 'pst' : v === 'editartist' ? 'ast' : null; if (!t) return;
  if (!onlyPrev && $('rvStyle')) $('rvStyle').innerHTML = revStyleUI(t, t !== 'pst');
  const el = $('rvPrev'); if (!el) return; const st = rev[t]; revPaint(el, st);
  if (t === 'st') {
    const body = ($('rvText').value || '').split(/\n{2,}/).slice(0, 2).join('\n\n').slice(0, 600) || 'Aquí se verá tu texto con esta estética.';
    el.innerHTML = revHeader($('rvTitle').value || 'Tu título', $('rvSub').value, rev.cover, st, '', Date.now()) + `<div class="rv-txt" data-dc="${st.dc}">${revFmt(body)}</div>`;
    dzPrev();
  } else if (t === 'pst') {
    el.innerHTML = `<div class="rv-prof-top">${revAv({ name: $('rvPName').value || '?', avatar: rev.pav }, 72)}<div><h2>${esc($('rvPName').value || 'Tu nombre')}</h2></div></div><p class="rv-bio">${esc($('rvPBio').value || 'Tu biografía.')}</p>`;
  } else {
    el.innerHTML = revHeader($('rvAName').value || 'Artista', [$('rvAGenre').value, $('rvAYears').value].filter(Boolean).join(' · '), rev.acover, st, '', Date.now()) + `<div class="rv-txt" data-dc="${st.dc}">${revFmt(($('rvABio').value || 'Biografía del artista.').slice(0, 400))}</div>`;
  }
}
let revDraftT = 0;
function revSaveDraft() {
  if (rev.view !== 'edit' || rev.editId) return; clearTimeout(revDraftT);
  revDraftT = setTimeout(() => { try { localStorage.setItem('rr_rev_draft', JSON.stringify({ title: $('rvTitle').value, subtitle: $('rvSub').value, body: $('rvText').value, tracks: rev.tracks, cover: rev.cover, st: rev.st, tags: $('rvTags') ? revTags($('rvTags').value) : [], gallery: (rev.gal || []).filter(u => !u.startsWith('data:')), dz: dzDraft() })); } catch {} }, 600);
}
function revFormat(f) {
  const t = $('rvText'), a = t.selectionStart, b = t.selectionEnd, s = t.value.slice(a, b);
  const wrap = (l, r) => { t.setRangeText(l + (s || 'texto') + r, a, b, 'select'); };
  const pre = p => { const st = t.value.lastIndexOf('\n', a - 1) + 1; t.setRangeText(p, st, st, 'end'); };
  if (f === 'b') wrap('**', '**'); else if (f === 'i') wrap('*', '*'); else if (f === 'h') pre('## '); else if (f === 'q') pre('> '); else if (f === 'l') pre('- ');
  t.focus(); revSaveDraft(); revStChanged(true);
}
async function revFind() {
  const q = revNk($('rvFind').value), box = $('rvRes'); if (q.length < 2) { box.innerHTML = ''; return; }
  const lib = await libAll(); rev.found = lib.filter(r => revNk(r.name + ' ' + r.artist).includes(q)).slice(0, 8);
  box.innerHTML = rev.found.map((r, i) => `<button type="button" class="alt rv-res" data-act="addtr" data-i="${i}">${esc(r.name)} <small>${esc(r.artist || '')}</small></button>`).join('') || '<p class="hint">No encontré nada en tu biblioteca.</p>';
}
async function revPublish(mode) {
  if (!(await revNeed())) return;
  const me = revMe(), title = $('rvTitle').value.trim(), body = $('rvText').value.trim();
  if (title.length < 3) return toast('Escribe un título (mínimo 3 letras).', true);
  if (body.length < 20) return toast('El texto es muy corto (mínimo 20 letras).', true);
  let cover = rev.cover || null;
  if (cover && cover.startsWith('data:')) { // la imagen va al almacén (el feed carga mucho más liviano)
    try { toast('Subiendo imagen…', false, 15000); cover = await revUploadPair(cover, 'c-' + revUid(), 1400, 640); }
    catch { if (cover.length > 400000) return toast('No se pudo subir la imagen. Intenta de nuevo.', true); } // si no hay almacén (base sin v2), sigue como antes
  }
  const row = { title, subtitle: $('rvSub').value.trim() || null, cover, body, tracks: rev.tracks.slice(0, 12) };
  const sd = revStDiff(rev.st); if (Object.keys(sd).length || rev.hadStyle) row.style = sd;
  if (!rev.v5off && $('rvTags')) {
    row.tags = revTags($('rvTags').value);
    const when = $('rvWhen').value ? new Date($('rvWhen').value) : null;
    if (when && isNaN(when)) return toast('Revisa la fecha programada.', true);
    row.status = mode === 'draft' ? 'draft' : 'published';
    if (when && when > new Date()) row.publish_at = when.toISOString(); else if (rev.editId || mode !== 'draft') row.publish_at = new Date(Date.now() - 60000).toISOString();
    const gal = [];
    for (const u of rev.gal || []) { if (u.startsWith('data:')) { try { toast('Subiendo fotos…', false, 20000); gal.push(await revUploadPair(u, 'g-' + revUid(), 1600, 520)); } catch { return toast('No se pudo subir una foto de la galería.', true); } } else gal.push(u); }
    row.gallery = gal.slice(0, 8);
  }
  if (!rev.v3 && $('rvRvOn')) { row.review = revReviewRead(); if ($('rvRvOn').checked && !row.review) return toast('Completa la reseña: disco y puntuación.', true); }
  try {
    let id = rev.editId;
    if (id) await revFetch(`/rest/v1/posts?id=eq.${id}`, { method: 'PATCH', body: row, prefer: 'return=minimal' });
    else { const r = await revFetch('/rest/v1/posts', { method: 'POST', body: { ...row, author_id: me.uid }, prefer: 'return=representation' }); id = r[0].id; try { localStorage.removeItem('rr_rev_draft'); } catch {} }
    let dzErr = ''; try { await dzSave(id); } catch (e) { dzErr = e.message; } if (dzErr) setTimeout(() => toast(dzErr, true, 9000), 1800);
    if (rev.editId && rev.editCover && rev.editCover !== cover) revUnPair(rev.editCover);
    if (rev.editId && row.gallery) (rev.editGal || []).filter(u => !row.gallery.includes(u)).forEach(revUnPair);
    if (row.status === 'draft' || (row.publish_at && new Date(row.publish_at) > new Date())) { toast(row.status === 'draft' ? 'Borrador guardado. Solo tú lo ves.' : 'Publicación programada.'); }
    else toast(rev.editId ? 'Cambios guardados.' : '¡Artículo publicado!'); rev.posts = []; rev.t = 0;
    if (rev.editId) rev.stack.pop(); // no volver al artículo viejo
    rev.view = 'feed'; revOpen(id, true);
  } catch (e) { toast(e.message, true, 6000); }
}

/* --- Perfiles de usuario --- */
async function revProfile(id, noPush) {
  if (!UUID.test(id)) return;
  if (!noPush) revPush();
  rev.sec = 'art'; rev.view = 'profile'; const my = ++rev.req, box = revBody(); box.innerHTML = '<p class="hint">Cargando…</p>'; revBar();
  if (!rev.prof || rev.prof.id !== id) rev.profTab = 'posts';   // lo primero que se ve es lo que la persona creó; el muro va aparte
  try {
    const me0 = revMe();
    const [pr, fol, fing, , , np] = await Promise.all([
      revFetch(`/rest/v1/profiles?select=*&id=eq.${id}`),
      revFetch(`/rest/v1/follows?select=follower_id&author_id=eq.${id}&limit=5000`),
      revFetch(`/rest/v1/follows?select=author_id&follower_id=eq.${id}&limit=5000`), revSocial(), vaultCurators().catch(() => {}),
      revFetch(`/rest/v1/posts?select=id&author_id=eq.${id}${me0 && me0.uid === id ? '' : REV_PUB()}&limit=1000`).catch(() => [])]);
    if (my !== rev.req) return;
    if (!pr[0]) { box.innerHTML = revHead('Volver', '') + '<p class="hint">Ese perfil no existe.</p>'; return; }
    rev.prof = { ...pr[0], nFol: fol.length, nFing: fing.length, nPosts: (np || []).length };
    revDrawProfile(); revProfTab(rev.profTab);
  } catch (e) { if (my === rev.req) box.innerHTML = revHead('Volver', '') + `<p class="hint">${esc(e.message)}</p>`; }
}
function revDrawProfile() {
  const p = rev.prof, me = revMe(), st = revSt(p.style), mine = me && me.uid === p.id;
  const role = [p.role === 'admin' ? 'Admin' : p.role === 'editor' ? 'Editor' : '', p.role !== 'admin' && rev.curators && rev.curators.has(p.id) ? 'Curador de la Bóveda' : ''].filter(Boolean).join(' · ');
  const adminBtn = revIsAdmin() && !mine && p.role !== 'admin' ? `<button type="button" class="alt" data-act="role" data-v="${p.role === 'editor' ? 'user' : 'editor'}">${p.role === 'editor' ? 'Quitar permiso de editor' : 'Hacer editor'}</button><button type="button" class="alt" data-act="aname" data-id="${esc(p.id)}">Cambiar nombre</button><button type="button" class="alt" data-act="ban" data-id="${esc(p.id)}" data-v="${p.banned ? 0 : 1}">${p.banned ? 'Reactivar cuenta' : 'Suspender cuenta'}</button><button type="button" class="alt" data-act="bvcur" data-id="${esc(p.id)}" data-v="${rev.curators && rev.curators.has(p.id) ? 0 : 1}">${rev.curators && rev.curators.has(p.id) ? 'Quitar curador de la Bóveda' : 'Hacer curador de la Bóveda'}</button>` : '';
  const ban = revPic(p.banner);
  revBody().innerHTML = revHead('Volver', '') + `<section class="rv-prof nv-prof rv-st" ${revAttrs(st)}>
<div class="nv-ban${ban ? '' : ' nv-ban0'}" style="--h:${nvHue(p.name)}">${ban ? `<img src="${esc(ban)}" alt="" decoding="async">` : ''}</div>
<div class="nv-pm"><div class="nv-prow"><span class="nv-pav">${revAv(p, 96)}</span><div class="nv-pact">${mine ? '<button type="button" class="alt" data-act="editprof">Editar perfil</button>' : revFollowBtn(p.id)}<button type="button" class="nv-ib nv-ibb" data-act="shareprof" aria-label="Compartir perfil">${ICO('share')}</button></div></div>
<div class="rv-prof-top"><div><h2>${esc(p.name)}</h2>${role ? `<span class="rv-role">${role}</span>` : ''}</div></div>
${p.banned ? '<p class="rv-warn">Cuenta suspendida</p>' : ''}<div id="rvBadges" class="rv-badges"></div>${p.bio ? `<p class="rv-bio">${esc(p.bio)}</p>` : ''}${nvNowHtml(p)}
<div class="nv-stats"><span><b>${p.nFol}</b> seguidores</span><span><b>${p.nFing}</b> siguiendo</span><span><b>${p.nPosts || 0}</b> artículos</span><span class="nv-since">desde ${revDate(p.created_at, { month: 'short', year: 'numeric' })}</span></div>
<div class="rv-pbtns nv-pbtns2">${mine ? '<button type="button" class="alt" data-act="logout">Cerrar sesión</button><button type="button" class="alt" data-act="rules">Normas</button>' : ''}${adminBtn}${mine ? '' : `<button type="button" class="rv-x" data-act="report" data-type="profile" data-id="${esc(p.id)}">Denunciar</button>`}</div></div></section>
<div class="rv-tabs nv-tabs">${[['posts', 'Artículos'], ['lists', 'Listas'], ['cols', 'Colecciones'], ['recs', 'Recomendados'], ['wall', 'Muro'], ['comments', 'Comentarios'], ['likes', 'Me gusta']].map(([k, n]) => `<button type="button" class="rv-tab${rev.profTab === k ? ' on' : ''}" data-act="ptab" data-v="${k}">${n}</button>`).join('')}</div>
<div id="rvPList"><p class="hint">Cargando…</p></div>`;
  revBadges(p);
}
async function revProfTab(k) {
  rev.profTab = k;
  document.querySelectorAll('#revBody .rv-tabs .rv-tab').forEach(b => b.classList.toggle('on', b.dataset.v === k));
  if (k === 'wall') return nvWall();
  const id = rev.prof.id, my = ++rev.req;
  try {
    let h;
    if (k === 'lists') { h = await lsProfTab(id, my); if (h === null) return; }
    else if (k === 'cols') { const me = revMe(); const a = await revFetch(`/rest/v1/collections?select=id,title,public,collection_items(count)&owner=eq.${id}&order=created_at.desc`); h = a.map(c => `<div class="rv-urow rv-click" data-act="collection" data-id="${esc(c.id)}" tabindex="0"><div><b>${esc(c.title)}</b><small>${revCnt(c.collection_items)} pósters${c.public ? '' : ' · privada'}</small></div></div>`).join('') || `<p class="hint">${me && me.uid === id ? 'Aún no tienes colecciones. Guarda pósters desde la galería.' : 'Sin colecciones públicas.'}</p>`; }
    else if (k === 'posts') { const me = revMe(); const a = await revFetch(`/rest/v1/posts?select=${REV_CARD()}&author_id=eq.${id}${me && me.uid === id ? '' : REV_PUB()}&order=created_at.desc&limit=30`); h = a.length ? revGrid(a) : '<p class="hint">Aún no ha publicado artículos.</p>'; }
    else if (k === 'comments') { const a = await revFetch(`/rest/v1/comments?select=id,body,created_at,post_id,posts(title)&author_id=eq.${id}&order=created_at.desc&limit=30`); h = a.map(c => `<div class="rv-c rv-click" data-act="open" data-id="${esc(c.post_id)}" tabindex="0"><p><small>En «${esc((c.posts && c.posts.title) || 'artículo')}» · ${revDate(c.created_at)}</small></p><p>${esc(c.body)}</p></div>`).join('') || '<p class="hint">Sin comentarios todavía.</p>'; }
    else if (k === 'recs') { const a = await revFetch(`/rest/v1/recommends?select=note,created_at,posts(${REV_CARD()})&user_id=eq.${id}&order=created_at.desc&limit=30`); const ps = a.filter(x => x.posts).map(x => ({ ...x.posts, _rec: { name: rev.prof.name, note: x.note } })); h = ps.length ? revGrid(ps) : '<p class="hint">Todavía no recomienda artículos.</p>'; }
    else { const a = await revFetch(`/rest/v1/likes?select=posts(${REV_CARD()})&user_id=eq.${id}&limit=30`); const ps = a.map(x => x.posts).filter(Boolean); h = ps.length ? revGrid(ps) : '<p class="hint">Todavía no le ha dado me gusta a nada.</p>'; }
    if (my === rev.req && $('rvPList')) $('rvPList').innerHTML = h;
  } catch (e) { if ($('rvPList')) $('rvPList').innerHTML = `<p class="hint">${esc(e.message)}</p>`; }
}
function revEditProfile() {
  if (rev.v1) return toast(V2_MSG, true, 7000);
  const p = rev.me || {}; revPush(); ++rev.req; rev.view = 'editprof'; rev.pst = revSt(p.style); rev.pav = revPic(p.avatar); rev.pban = revPic(p.banner) || ''; revBar();
  revBody().innerHTML = revHead('Cancelar', 'Editar perfil') + `<div class="rv-form">
<label for="rvPName">Nombre público</label><input id="rvPName" maxlength="40" autocomplete="off" value="${esc(p.name || '')}">
<label for="rvPBio">Biografía (opcional)</label><textarea id="rvPBio" rows="3" maxlength="300" placeholder="Qué música te mueve, de dónde eres…">${esc(p.bio || '')}</textarea>
<label>Foto</label><div class="row"><button type="button" class="alt" data-act="pickav">Elegir foto</button><button type="button" class="alt" data-act="noav">Quitar</button></div><input type="file" id="rvAvFile" accept="image/*" hidden>
<label>Imagen de fondo</label><div id="nvBanPrev" class="nv-banprev"></div><div class="row"><button type="button" class="alt" data-act="nvpickban">Elegir imagen de fondo</button><button type="button" class="alt" data-act="nvnoban">Quitar</button></div><input type="file" id="nvBanFile" accept="image/*" hidden>
<label class="chk"><input type="checkbox" id="nvWallOpen"${p.wall_open === false ? '' : ' checked'}> Otras personas pueden escribir en mi muro</label>
<label class="chk"><input type="checkbox" id="nvNowShare"${store.get('nowShare', '') === '1' ? ' checked' : ''}> Mostrar en mi perfil lo que estoy escuchando en la radio</label>
<label>Estética de tu perfil</label><div id="rvStyle"></div><div id="rvPrev" class="rv-prof rv-st"></div>
<div class="row" style="margin-top:14px"><button type="button" data-act="saveprof">Guardar perfil</button></div></div>`;
  revStChanged(); nvDrawBan();
  $('nvBanFile').onchange = async e => { const f = e.target.files && e.target.files[0]; e.target.value = ''; if (!f) return; try { rev.pban = (await revResize(f, 1500, 0.84)).data(); nvDrawBan(); } catch { toast('No se pudo leer esa imagen.', true); } };
}
async function revSaveProfile() {
  const me = revMe(); if (!me) return;
  const name = $('rvPName').value.trim().slice(0, 40), bio = $('rvPBio').value.trim().slice(0, 300);
  if (name.length < 2) return toast('Tu nombre debe tener al menos 2 letras.', true);
  const old = (rev.me || {}).avatar; let avatar = rev.pav || null;
  if (avatar && avatar.startsWith('data:')) { try { avatar = await revUpload(revDataBlob(avatar), `av-${revUid()}.jpg`); } catch {} } // si no hay almacén, se guarda como antes
  const oldBan = (rev.me || {}).banner; let banner = rev.pban || null;
  if (banner && banner.startsWith('data:')) { try { banner = await revUploadPair(banner, 'fondo-' + revUid(), 1500, 600); } catch (e) { return toast('No se pudo subir la imagen de fondo: ' + e.message, true, 6000); } }
  const body = { name, bio: bio || null, avatar, style: revStDiff(rev.pst) };
  if (!rev.v7off) { body.banner = banner; body.wall_open = $('nvWallOpen').checked; }
  const share = $('nvNowShare').checked; store.set('nowShare', share ? '1' : ''); if (!share && !rev.v8off && (rev.me || {}).now_playing) body.now_playing = null;
  try {
    let r;
    try { r = await revFetch(`/rest/v1/profiles?id=eq.${me.uid}`, { method: 'PATCH', body, prefer: 'return=representation' }); }
    catch (e) { if (rev.v7off || !/banner|wall_open|now_playing|schema cache|column/i.test(e.message + (e.raw || ''))) throw e; if ('now_playing' in body && /now_playing/.test(e.message + (e.raw || ''))) { rev.v8off = true; delete body.now_playing; } else { rev.v7off = true; delete body.banner; delete body.wall_open; delete body.now_playing; toast(V7_MSG, true, 7000); } r = await revFetch(`/rest/v1/profiles?id=eq.${me.uid}`, { method: 'PATCH', body, prefer: 'return=representation' }); }
    if (old && old !== avatar) revUnPair(old);
    if (!rev.v7off && oldBan && oldBan !== banner) revUnPair(oldBan);
    if (r && r[0]) rev.me = { ...rev.me, ...r[0] };
    revSaveSes({ ...revMe(), name }); toast('Perfil guardado.'); revBack();
  } catch (e) { toast(e.message, true, 6000); }
}

/* --- Perfiles de artistas (solo admin y editores los crean o editan) --- */
async function revArtists() {
  rev.sec = 'artists'; rev.view = 'artists'; revBar(); const my = ++rev.req, box = revBody();
  if (rev.v1) { box.innerHTML = `<p class="hint">${V2_MSG}</p>`; return; }
  box.innerHTML = `<input id="rvASearch" class="rv-search" type="search" placeholder="Buscar artista, banda o género…" value="${esc(rev.aQ)}" autocomplete="off"><div id="rvAList"><p class="hint">Cargando…</p></div>`;
  try {
    rev.artists = await revFetch('/rest/v1/artists?select=id,name,genre,years,cover,style,verified&order=name.asc&limit=1000').catch(() => revFetch('/rest/v1/artists?select=id,name,genre,years,cover,style&order=name.asc&limit=1000')); rev.aT = Date.now();
    if (my === rev.req) revDrawArtists();
  } catch (e) { if (my === rev.req && $('rvAList')) $('rvAList').innerHTML = `<p class="hint">${esc(e.message)}</p><button type="button" class="alt" data-act="retry">Reintentar</button>`; }
}
function revDrawArtists() {
  const el = $('rvAList'); if (!el || !rev.artists) return;
  const q = revNk(rev.aQ), list = rev.artists.filter(a => !q || revNk(a.name + ' ' + (a.genre || '') + ' ' + (a.years || '')).includes(q));
  el.innerHTML = list.length ? `<div class="rv-agrid">${list.map(a => { const st = revSt(a.style, { cv: 'm' }), im = revUrl(a.cover); return `<article class="rv-acard rv-st" ${revAttrs(st)} data-act="artist" data-id="${esc(a.id)}" tabindex="0">${im ? revImgTag(im) : `<div class="rv-ph"><span>${esc(a.name.slice(0, 1).toUpperCase())}</span></div>`}<div class="rv-in"><h3>${esc(a.name)}${a.verified ? ' <span class="rv-ok" title="Banda verificada">✓</span>' : ''}</h3><p class="rv-m">${esc([a.genre, a.years].filter(Boolean).join(' · '))}</p></div></article>`; }).join('')}</div>`
    : `<p class="hint">${rev.artists.length ? 'No encontré ese artista.' : 'Aún no hay perfiles de artistas.'}${revIsEditor() ? ' Toca + para crear uno.' : ''}</p>`;
}
async function revArtist(id, noPush) {
  if (!UUID.test(id)) return;
  if (!noPush) revPush();
  rev.sec = 'artists'; rev.view = 'artist'; revBar(); const my = ++rev.req, box = revBody(); box.innerHTML = '<p class="hint">Cargando…</p>';
  try {
    const a = (await revFetch(`/rest/v1/artists?select=*&id=eq.${id}`))[0];
    if (!a) { box.innerHTML = revHead('Volver', '') + '<p class="hint">Ese artista ya no existe.</p>'; return; }
    const pat = revPat(a.name), w = encodeURIComponent('*' + pat + '*');
    const [ps, arts] = await Promise.all([
      pat ? revFetch(`/rest/v1/posters?select=id,title,artist,year,kind,thumb_url,image_url&artist=ilike.${w}&order=year.asc.nullslast&limit=30`) : [],
      pat ? revFetch(`/rest/v1/posts?select=${REV_CARD()}&or=${encodeURIComponent(`(title.ilike.*${pat}*,subtitle.ilike.*${pat}*)`)}${REV_PUB()}&order=created_at.desc&limit=12`) : []]);
    if (my !== rev.req) return;
    rev.artist = a; rev.aPosters = ps; rev.aPosts = arts; await revDrawArtist();
  } catch (e) { if (my === rev.req) box.innerHTML = revHead('Volver', '') + `<p class="hint">${esc(e.message)}</p>`; }
}
async function revDrawArtist() {
  const a = rev.artist, st = revSt(a.style, { cv: 'm' }), ed = revIsEditor();
  let n = 0; try { n = (await libAll()).filter(r => revNk(r.artist) === revNk(a.name)).length; } catch {}
  const info = [a.genre, a.years, a.origin].filter(Boolean).join(' · '), L = revLinks(a.links);
  revBody().innerHTML = revHead('Artistas', '', `<span class="rv-sp"></span>${ed ? '<button type="button" class="alt" data-act="editartist">Editar</button><button type="button" class="alt" data-act="delartist">Eliminar</button>' : ''}`) +
`<article class="rv-art rv-st" ${revAttrs(st)}>${revHeader(a.name, info, a.cover, st, '', a.updated_at)}
${a.verified ? '<p class="rv-verif"><span class="rv-ok">✓</span> Banda verificada por la Revista</p>' : ''}${Object.keys(L).length ? `<div class="rv-links">${REV_LINKS.filter(([k]) => L[k]).map(([k, n]) => `<a href="${esc(L[k])}" target="_blank" rel="noopener noreferrer">${n} ↗</a>`).join('')}</div>` : ''}
<div class="rv-txt" data-dc="${st.dc}">${revFmt(a.bio || '') || '<p class="hint">Sin biografía todavía.</p>'}</div>
<div class="rv-acts">${n ? `<button type="button" class="rv-like" data-act="playartist">${ICO('play')} Escuchar ${n} ${n === 1 ? 'canción' : 'canciones'} de tu biblioteca</button>` : ''}<button type="button" class="rv-like" data-act="shareartist">${ICO('share')} Compartir</button></div></article>
<div id="rvMgr"></div>
<h4 class="rv-h">Reseñas de la comunidad</h4><div id="rvAReviews"><p class="hint">Cargando…</p></div>
<h4 class="rv-h">Discografía</h4><div id="rvDisco"></div>
<h4 class="rv-h">Pósters</h4>${rev.aPosters.length ? `<div class="rv-prow">${rev.aPosters.map(p => `<figure class="rv-pmini" data-act="poster" data-id="${esc(p.id)}" tabindex="0"><img src="${esc(revUrl(p.thumb_url) || revUrl(p.image_url))}" alt="" loading="lazy" decoding="async"><figcaption>${p.year || ''} ${p.kind === 'original' ? '· Original' : '· Comunidad'}</figcaption></figure>`).join('')}</div>` : '<p class="hint">Aún no hay pósters de este artista. Búscalos en la pestaña Pósters.</p>'}
<h4 class="rv-h">En la Revista</h4>${rev.aPosts.length ? revGrid(rev.aPosts) : '<p class="hint">Todavía no hay artículos que lo mencionen en el título.</p>'}`;
  revBody().scrollIntoView({ block: 'start' });
  if (!rev.v3) { revManagers(a); revArtistReviews(a); } revDisco(a);
}
function revEditArtist(a) {
  const me = revMe(), mgr = a && me && rev.aManagers.includes(me.uid);
  if (!revIsEditor() && !mgr) return toast('Solo el administrador, sus editores y los gestores de la banda pueden editar artistas.', true);
  revPush(); ++rev.req; rev.view = 'editartist'; rev.editA = a ? a.id : null; rev.ast = revSt(a && a.style, { cv: 'm' }); rev.acover = a ? revUrl(a.cover) : ''; revBar();
  const v = k => esc((a && a[k]) || '');
  revBody().innerHTML = revHead('Cancelar', a ? 'Editar artista' : 'Nuevo artista') + `<div class="rv-form">
<label for="rvAName">Nombre del artista o banda</label><input id="rvAName" maxlength="80" autocomplete="off" value="${v('name')}">
<div class="rv-two"><label>Género<input id="rvAGenre" maxlength="60" autocomplete="off" value="${v('genre')}" placeholder="Grunge"></label><label>Años<input id="rvAYears" maxlength="40" autocomplete="off" value="${v('years')}" placeholder="1987–1994"></label></div>
<label for="rvAOrigin">Origen</label><input id="rvAOrigin" maxlength="60" autocomplete="off" value="${v('origin')}" placeholder="Aberdeen, Washington">
${rev.v3 ? '' : `<p class="rv-sl">Enlaces oficiales (https://…)</p><div class="rv-two">${REV_LINKS.map(([k, n]) => `<label>${n}<input id="rvL_${k}" maxlength="300" autocomplete="off" placeholder="https://" value="${esc(revLinks(a && a.links)[k] || '')}"></label>`).join('')}</div>`}
<label for="rvABio">Biografía</label><textarea id="rvABio" rows="8" maxlength="6000" placeholder="Su historia, sus discos clave…">${v('bio')}</textarea>
<label>Imagen de portada</label><div class="row"><button type="button" class="alt" data-act="pickacover">Elegir imagen</button><button type="button" class="alt" data-act="noacover">Quitar</button></div><input type="file" id="rvACovFile" accept="image/*" hidden><p class="hint" id="rvACovSt">${rev.acover ? 'Con imagen.' : 'Sin imagen.'}</p>
<label>Estética del artista</label><div id="rvStyle"></div><div id="rvPrev" class="rv-art rv-st"></div>
<div class="row" style="margin-top:14px"><button type="button" data-act="saveartist">${a ? 'Guardar cambios' : 'Crear artista'}</button></div></div>`;
  revStChanged();
}
async function revSaveArtist() {
  const name = $('rvAName').value.trim();
  if (!name) return toast('Escribe el nombre del artista.', true);
  const row = { name, genre: $('rvAGenre').value.trim() || null, years: $('rvAYears').value.trim() || null, origin: $('rvAOrigin').value.trim() || null, bio: $('rvABio').value.trim() || null, cover: rev.acover || null, style: revStDiff(rev.ast), updated_at: new Date().toISOString() };
  if (!rev.v3 && $('rvL_web')) { const o = {}; REV_LINKS.forEach(([k]) => { o[k] = $('rvL_' + k).value; }); const L = revLinks(o); if (REV_LINKS.some(([k]) => o[k].trim() && !L[k])) return toast('Los enlaces deben empezar con https://', true); row.links = L; }
  try {
    let id = rev.editA;
    if (id) await revFetch(`/rest/v1/artists?id=eq.${id}`, { method: 'PATCH', body: row, prefer: 'return=minimal' });
    else { const r = await revFetch('/rest/v1/artists', { method: 'POST', body: { ...row, created_by: revMe().uid }, prefer: 'return=representation' }); id = r[0].id; }
    toast(rev.editA ? 'Artista actualizado.' : 'Artista creado.'); rev.artists = null; rev.aT = 0;
    if (rev.editA) rev.stack.pop();
    rev.view = 'artists'; revArtist(id, true);
  } catch (e) { toast(e.message, true, 6000); }
}
async function revArtistCover(file) {
  try {
    if (!(await revNeed())) return;
    $('rvACovSt').textContent = 'Subiendo imagen…';
    rev.acover = await revUploadPair(file, `a-${revUid()}`, 1400, 520);
    $('rvACovSt').textContent = 'Con imagen.'; revStChanged(true);
  } catch (e) { $('rvACovSt').textContent = 'No se pudo subir.'; toast(e.message, true, 6000); }
}

/* --- Pósters: originales (Wikimedia Commons, licencia libre) y de la comunidad (subidos o con IA) --- */
const REV_PSEL = () => 'id,title,artist,year,kind,source,thumb_url,image_url,credit,designer,license,source_url,challenge_id,created_by,created_at,profiles!created_by(name)';
function revPosters(reset) {
  rev.sec = 'posters'; rev.view = 'posters'; revBar();
  if (rev.v1) { revBody().innerHTML = `<p class="hint">${V2_MSG}</p>`; return; }
  revBody().innerHTML = `<input id="rvPSearch" class="rv-search" type="search" placeholder="Buscar por banda, título o año…" value="${esc(rev.pQ)}" autocomplete="off">
<div class="rv-tabs">${[['all', 'Todos'], ['original', 'Originales'], ['comunidad', 'De la comunidad'], ['retos', 'Retos']].map(([k, n]) => `<button type="button" class="rv-tab${rev.pFilter === k ? ' on' : ''}" data-act="pfilter" data-v="${k}">${n}</button>`).join('')}</div><div id="rvPGrid"></div>`;
  if (rev.pFilter === 'retos') return revChallenges();
  if (reset !== false || !rev.posters.length) return revPLoad(false);
  revDrawPosters();
}
async function revPLoad(more) {
  const my = ++rev.req, el = $('rvPGrid'); if (!el) return;
  if (!more) { rev.pOff = 0; rev.posters = []; el.innerHTML = '<p class="hint">Cargando…</p>'; }
  let q = `/rest/v1/posters?select=${REV_PSEL()}&order=created_at.desc&limit=20&offset=${rev.pOff}`;
  if (rev.pFilter !== 'all') q += `&kind=eq.${rev.pFilter}`;
  const t = revPat(rev.pQ);
  if (/^(1[89]\d\d|20\d\d)$/.test(t)) q += `&year=eq.${t}`;
  else if (t) q += `&or=${encodeURIComponent(`(title.ilike.*${t}*,artist.ilike.*${t}*)`)}`;
  try {
    const a = await revFetch(q); if (my !== rev.req) return;
    rev.posters = rev.posters.concat(a); rev.pOff += a.length; rev.pMore = a.length === 20; revDrawPosters();
  } catch (e) { if (my === rev.req && $('rvPGrid')) $('rvPGrid').innerHTML = `<p class="hint">${esc(e.message)}</p><button type="button" class="alt" data-act="retry">Reintentar</button>`; }
}
const revBadge = p => `<span class="rv-badge ${p.kind === 'original' ? 'og' : 'cm'}">${p.kind === 'original' ? 'Original' : 'De la comunidad'}</span>`;
function revDrawPosters() {
  const el = $('rvPGrid'); if (!el) return;
  el.innerHTML = (rev.posters.length ? `<div class="rv-pgrid">${rev.posters.map((p, i) => `<figure class="rv-pcard" data-act="poster" data-i="${i}" tabindex="0"><img src="${esc(revUrl(p.thumb_url) || revUrl(p.image_url))}" alt="${esc(p.title)}" loading="lazy" decoding="async"><figcaption>${revBadge(p)}<b>${esc(p.title)}</b><small>${esc([p.artist, p.year].filter(Boolean).join(' · ') || ' ')}</small></figcaption></figure>`).join('')}</div>`
    : `<p class="hint">${rev.pQ ? 'No encontré pósters con esa búsqueda.' : 'La galería está vacía.'} Toca + para buscar originales con licencia libre, subir uno o crearlo con IA.</p>`) +
    (rev.pMore ? '<button type="button" class="alt rv-more" data-act="pmore">Cargar más</button>' : '');
}
async function revPosterOpen(x, noPush) {
  let p = typeof x === 'object' ? x : null;
  if (!noPush) revPush();
  rev.sec = 'posters'; rev.view = 'poster'; revBar(); const my = ++rev.req, box = revBody();
  if (!p) {
    if (!UUID.test(x)) return; box.innerHTML = '<p class="hint">Cargando…</p>';
    try { p = (await revFetch(`/rest/v1/posters?select=${REV_PSEL()}&id=eq.${x}`))[0]; } catch (e) { box.innerHTML = revHead('Volver', '') + `<p class="hint">${esc(e.message)}</p>`; return; }
    if (my !== rev.req) return;
    if (!p) { box.innerHTML = revHead('Volver', '') + '<p class="hint">Ese póster ya no está en la galería.</p>'; return; }
  }
  rev.poster = p; const me = revMe(), mine = me && me.uid === p.created_by;
  let art = null; try { if (!rev.artists) rev.artists = await revFetch('/rest/v1/artists?select=id,name,genre,years,cover,style&order=name.asc&limit=1000'); art = p.artist && rev.artists.find(a => revNk(a.name) === revNk(p.artist)); } catch {}
  if (my !== rev.req) return;
  const autor = (() => { const c = String(p.credit || '').trim(); const h = c.length % 2 === 0 ? c.slice(0, c.length / 2) : ''; return (h && h + h === c ? h : c) || 'desconocido'; })();
  const cred = p.kind === 'original'
    ? `<p class="rv-m">Autor: ${esc(autor)}${p.license ? ` · Licencia: ${esc(p.license)}` : ''}</p><p class="rv-m"><a href="${esc(p.source_url)}" target="_blank" rel="noopener noreferrer">Ver la ficha en Wikimedia Commons</a> · guardado por ${esc(revName(p))}</p>`
    : `${p.designer ? `<p class="rv-m">Arte de <b>${esc(p.designer)}</b></p>` : ''}<p class="rv-m">${p.source === 'ia' ? 'Creado con IA (antes de que la revista dejara de aceptarlo)' : 'Subido'} por <a href="#" data-act="profile" data-id="${esc(p.created_by)}">${esc(revName(p))}</a> · ${revDate(p.created_at)}</p>`;
  box.innerHTML = revHead('Volver', '', `<span class="rv-sp"></span>${mine || revIsAdmin() ? '<button type="button" class="alt" data-act="delp">Eliminar</button>' : ''}`) +
`<figure class="rv-pview"><img src="${esc(revUrl(p.image_url))}" alt="${esc(p.title)}"><figcaption>${revBadge(p)}<h2>${esc(p.title)}</h2><p class="rv-sub">${esc([p.artist, p.year].filter(Boolean).join(' · '))}</p>${cred}
<div class="rv-acts"><button type="button" class="rv-like" data-act="dl">${ICO('download')} Descargar</button><button type="button" class="rv-like" data-act="sharep">${ICO('share')} Compartir</button><button type="button" class="rv-like" data-act="colsave">${ICO('plus')} Guardar en colección</button><button type="button" class="rv-like" data-act="wall">${ICO('image')} Fondo de pantalla</button><button type="button" class="rv-like" data-act="story" data-v="poster">Imagen para historias</button>${p.kind === 'original' && p.artist ? '<button type="button" class="rv-like" data-act="compare">Comparar con la comunidad</button>' : ''}${art ? `<button type="button" class="rv-like" data-act="artist" data-id="${esc(art.id)}">Ver perfil de ${esc(art.name)}</button>` : ''}${mine ? '' : `<button type="button" class="rv-x" data-act="report" data-type="poster" data-id="${esc(p.id)}">Denunciar</button>`}</div></figcaption></figure>`;
  revPosterExtra(p, art, my);
  box.scrollIntoView({ block: 'start' });
}
/* Ficha del póster: detalles de la imagen y del artista, y tocar la imagen para verla en grande */
function revPosterExtra(p, art, my) {
  const fig = document.querySelector('#revBody .rv-pview'), img = fig && fig.querySelector('img'); if (!img) return;
  img.style.cursor = 'zoom-in'; img.title = 'Toca para ver en grande';
  img.addEventListener('click', () => { const lb = document.createElement('div'); lb.className = 'rv-lb'; lb.setAttribute('role', 'dialog'); lb.setAttribute('aria-label', 'Póster en grande'); lb.innerHTML = `<img src="${esc(img.currentSrc || img.src)}" alt="${esc(p.title)}">`; lb.onclick = () => lb.remove(); document.body.appendChild(lb); });
  const filas = [];
  const dominio = (() => { try { return new URL(p.source_url || p.image_url).hostname.replace(/^www\./, ''); } catch { return ''; } })();
  if (p.year) filas.push(['Año', String(p.year)]);
  if (p.artist) filas.push(['Artista', p.artist + (art && (art.genre || art.years) ? ' · ' + [art.genre, art.years].filter(Boolean).join(' · ') : '')]);
  if (p.kind === 'original') filas.push(['Tipo', 'Póster original de la época']); else filas.push(['Tipo', p.source === 'ia' ? 'Creado con IA' : 'Diseño de la comunidad']);
  if (p.license) filas.push(['Licencia', p.license]);
  if (dominio) filas.push(['Fuente', dominio]);
  filas.push(['En la galería desde', revDate(p.created_at)]);
  const det = document.createElement('dl'); det.className = 'rv-pdet';
  const pintar = () => { det.innerHTML = filas.map(([k, v]) => `<div><dt>${esc(k)}</dt><dd>${esc(v)}</dd></div>`).join(''); };
  pintar(); const cap = fig.querySelector('figcaption'); const acts = cap && cap.querySelector('.rv-acts'); if (acts) cap.insertBefore(det, acts); else if (cap) cap.appendChild(det);
  const medir = () => { if (my !== rev.req || !img.naturalWidth) return; const w = img.naturalWidth, h = img.naturalHeight; filas.splice(filas.length - 1, 0, ['Tamaño', `${w} × ${h} px · ${w > h ? 'horizontal' : w < h ? 'vertical' : 'cuadrado'}`]); pintar(); };
  if (img.complete) medir(); else img.addEventListener('load', medir, { once: true });
  if (art && art.id) { const b = document.createElement('button'); b.type = 'button'; b.className = 'rv-like'; b.dataset.act = 'artist'; b.dataset.id = art.id; b.textContent = 'Ver la ficha de ' + art.name; if (acts) acts.appendChild(b); }
}
async function revDelPoster() {
  const p = rev.poster; if (!p || !(await uiConfirm('¿Quitar este póster de la galería?', 'No se puede deshacer.', 'Eliminar'))) return;
  try {
    await revFetch(`/rest/v1/posters?id=eq.${p.id}`, { method: 'DELETE' });
    if (p.source !== 'commons') { await revUnupload(p.image_url); await revUnupload(p.thumb_url); }
    toast('Póster eliminado.'); rev.posters = rev.posters.filter(x => x.id !== p.id); revBack();
  } catch (e) { toast(e.message, true, 6000); }
}
async function revPosterMenu() {
  const o = await uiDialog({ title: 'Añadir un póster', text: 'Busca pósters reales con su fuente, o sube un diseño tuyo. En la Revista el arte lo hacen personas.', options: [{ label: 'Buscar pósters (web y archivo libre)', value: 'find' }, { label: 'Subir un póster mío', value: 'upload' }], cancel: 'Cancelar' });
  if (o === 'find') revCommonsView(); else if (o === 'upload') revUploadPoster();
}
function revCommonsView() {
  revPush(); ++rev.req; rev.view = 'commons'; revBar();
  revBody().innerHTML = revHead('Volver', 'Buscar pósters') + `<p class="hint">Busco en dos lugares: el <b>archivo libre</b> de Wikimedia Commons (imágenes con licencia libre que puedes guardar en la galería) y, con tu IA, <b>páginas oficiales y de la comunidad</b>. De la web solo muestro el enlace a la fuente: nunca copio la imagen.</p>
<div class="rv-qrow"><input id="rvCQ" type="search" placeholder="Ej: Woodstock, Nirvana 1991, Soda Stereo" value="${esc(rev.cq)}" autocomplete="off"><button type="button" data-act="csearch">Buscar</button></div><div id="rvWRes"></div><div id="rvCRes"></div><div id="rvARes"></div>`;
  if (rev.cq) revCommons();
}
async function revCommons() {
  const q = $('rvCQ').value.trim(), el = $('rvCRes'); if (q.length < 2) return toast('Escribe al menos 2 letras.');
  rev.cq = q; el.innerHTML = '<h4 class="rv-h">Archivo libre · Wikimedia Commons</h4><p class="hint">Buscando…</p>'; const my = ++rev.req;
  revWebSearch(q, my); revArchive(q, my);
  const u = 'https://commons.wikimedia.org/w/api.php?action=query&format=json&origin=*&generator=search&gsrnamespace=6&gsrlimit=30&gsrsearch=' + encodeURIComponent(`${q} poster filetype:bitmap`) + '&prop=imageinfo&iiprop=url|extmetadata|mime&iiurlwidth=500&iiextmetadatafilter=LicenseShortName|Artist|DateTimeOriginal|ObjectName';
  try {
    const r = await fetch(u); if (!r.ok) throw new Error('Wikimedia no respondió (' + r.status + ').');
    const d = await r.json(); if (my !== rev.req) return;
    const pages = Object.values((d.query || {}).pages || {}).sort((a, b) => (a.index || 0) - (b.index || 0));
    rev.found = pages.map(p => {
      const ii = (p.imageinfo || [])[0] || {}, m = ii.extmetadata || {}, tx = k => revTxt((m[k] || {}).value || '');
      const y = (/\b(1[89]\d\d|20\d\d)\b/.exec(tx('DateTimeOriginal') + ' ' + p.title) || [])[1];
      return { title: (tx('ObjectName') || String(p.title || '').replace(/^File:/, '').replace(/\.[a-z0-9]+$/i, '')).slice(0, 120), url: ii.url, thumb: ii.thumburl || ii.url, src: ii.descriptionurl, license: tx('LicenseShortName').slice(0, 120), credit: tx('Artist').slice(0, 300), year: y ? +y : null, mime: ii.mime };
    }).filter(x => x.url && /^https:\/\/upload\.wikimedia\.org\//.test(x.url) && /^https:\/\/upload\.wikimedia\.org\//.test(x.thumb || '') && /^https:\/\/commons\.wikimedia\.org\//.test(x.src || '') && /^image\/(jpeg|png|webp)$/.test(x.mime || ''));
    el.innerHTML = '<h4 class="rv-h">Archivo libre · Wikimedia Commons</h4>' + (rev.found.length ? `<div class="rv-pgrid">${rev.found.map((x, i) => `<figure class="rv-pcard"><img src="${esc(x.thumb)}" alt="" loading="lazy" decoding="async"><figcaption><span class="rv-badge og">Original</span><b>${esc(x.title)}</b><small>${esc([x.year, x.license].filter(Boolean).join(' · '))}</small><button type="button" class="alt" data-act="csave" data-i="${i}">Guardar en la galería</button></figcaption></figure>`).join('')}</div>`
      : '<p class="hint">No encontré pósters con licencia libre para esa búsqueda. Prueba con otro nombre, un festival o un año.</p>');
  } catch (e) { if (my === rev.req) el.innerHTML = `<h4 class="rv-h">Archivo libre · Wikimedia Commons</h4><p class="hint">${esc(revErr(e.message))}</p>`; }
}
/* La IA de cada quien BUSCA pósters en la web (Gemini + Búsqueda de Google). Solo se muestra lo que
   está respaldado por una fuente citada; autor/diseño solo si la fuente lo confirma. Nunca se copia la imagen. */
async function revWebSearch(q, my) {
  const el = $('rvWRes'); if (!el) return;
  const head = '<h4 class="rv-h">En la web · páginas oficiales y de la comunidad</h4>';
  const key = getApiKey('geminiKey');
  if (!key) { el.innerHTML = head + '<p class="hint">Para que tu IA busque en la web, pon tu clave de Gemini en Ajustes › Sistema y Claves (API).</p>'; return; }
  el.innerHTML = head + '<p class="hint">Tu IA está buscando y revisando fuentes…</p>';
  const prompt = `Busca en la web pósters o carteles reales (conciertos, giras, festivales o discos) relacionados con: "${q}". Prioriza páginas oficiales del artista o del sello, museos, archivos, y sitios de coleccionistas o de la comunidad.
Responde SOLO con hasta 8 líneas, una por póster, con este formato exacto:
TÍTULO | AÑO | ARTISTA O BANDA | DISEÑADOR O AUTOR DEL PÓSTER | URL DE LA PÁGINA DONDE APARECE
Escribe "?" en cualquier campo que la página no confirme. No inventes datos ni URLs.`;
  try {
    const model = setting('geminiModel', '') || setting('geminiModelSelect', 'gemini-3.5-flash-lite');
    const r = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`, { method: 'POST', headers: { 'content-type': 'application/json', 'x-goog-api-key': key }, body: JSON.stringify({ contents: [{ parts: [{ text: prompt }] }], tools: [{ google_search: {} }] }) });
    const d = await r.json().catch(() => ({}));
    if (!r.ok) throw new Error(/quota|RESOURCE_EXHAUSTED|429/i.test(((d.error || {}).message || '') + r.status) ? 'Tu clave de Gemini no tiene cuota ahora. Prueba más tarde.' : ((d.error || {}).message || 'Error ' + r.status));
    if (my !== rev.req || !$('rvWRes')) return;
    const c = (d.candidates || [])[0] || {}, gm = c.groundingMetadata || {}, chunks = (gm.groundingChunks || []).map(x => x.web || {}), sup = gm.groundingSupports || [];
    const text = ((c.content || {}).parts || []).map(p => p.text || '').join('');
    const host = u => { try { return new URL(u).hostname.replace(/^www\./, ''); } catch { return ''; } };
    const clean = v => { v = String(v || '').trim(); return !v || /^\?+$|^(n\/a|desconocido|unknown)$/i.test(v) ? '' : v; };
    const items = [];
    text.split('\n').forEach(line => {
      const f = line.split('|').map(x => x.trim()); if (f.length < 5 || /^t[ií]tulo$/i.test(f[0])) return;
      const [title, year, artist, designer, url] = f.map(clean); if (!title) return;
      // ¿Qué fuentes respaldan esta línea?
      const ids = new Set(); sup.forEach(s => { const t = ((s.segment || {}).text || '').trim(); if (t && (line.includes(t) || t.includes(title))) (s.groundingChunkIndices || []).forEach(i => ids.add(i)); });
      if (!ids.size) return; // sin fuente que lo respalde: no se muestra
      const src = [...ids].map(i => chunks[i]).filter(x => x && x.uri);
      const hs = src.map(x => (x.title || '').replace(/^www\./, '').toLowerCase());
      const h = host(url), match = h && hs.some(x => x && (h === x || h.endsWith('.' + x) || x.endsWith('.' + h)));
      const link = match && /^https:\/\//.test(url) ? url : (src[0] || {}).uri;
      if (!/^https:\/\//.test(link || '')) return;
      items.push({ title: title.slice(0, 140), year: /^(1[89]\d\d|20\d\d)$/.test(year) ? year : '', artist: artist.slice(0, 80), designer: designer.slice(0, 80), link, site: match ? h : (hs[0] || host(link)) });
    });
    const all = [...new Map(chunks.filter(x => x.uri).map(x => [x.title, x])).values()].slice(0, 10);
    $('rvWRes').innerHTML = head + (items.length ? `<div class="rv-web">${items.map(x => `<a class="rv-wcard" href="${esc(x.link)}" target="_blank" rel="noopener noreferrer"><span class="rv-wyear">${esc(x.year || '—')}</span><b>${esc(x.title)}</b>
<small>${x.artist ? `Banda: ${esc(x.artist)} · ` : ''}${x.designer ? `Diseño: ${esc(x.designer)} <i title="Confirmado por la fuente">✓</i>` : 'Diseño: sin confirmar'}</small><small class="rv-src">Fuente: ${esc(x.site)} ↗</small></a>`).join('')}</div>`
      : '<p class="hint">Tu IA no encontró pósters respaldados por fuentes para esa búsqueda.</p>') +
      (all.length ? `<p class="rv-m rv-srcs">Fuentes consultadas: ${all.map(x => `<a href="${esc(x.uri)}" target="_blank" rel="noopener noreferrer">${esc(x.title || host(x.uri))}</a>`).join(' · ')}</p>` : '') +
      '<p class="hint">Solo muestro datos que la fuente respalda. Las imágenes de la web pertenecen a sus autores: ábrelas en su página.</p>';
  } catch (e) { if (my === rev.req && $('rvWRes')) $('rvWRes').innerHTML = head + `<p class="hint">${esc(e.message)}</p>`; }
}
async function revCommonsSave(i) {
  const x = rev.found[i]; if (!x || !(await revNeed())) return;
  const v = await uiDialog({ title: 'Guardar póster original', text: `Autor: ${x.credit || 'desconocido'} · Licencia: ${x.license || 'ver ficha'}`, fields: [{ key: 'title', label: 'Título', value: x.title }, { key: 'artist', label: 'Banda o artista', value: rev.cq }, { key: 'year', label: 'Año', value: x.year || '', mode: 'numeric' }], ok: 'Guardar' });
  if (!v) return;
  const y = parseInt(v.year, 10);
  try {
    await revFetch('/rest/v1/posters', { method: 'POST', prefer: 'return=minimal', body: { title: (v.title || x.title).trim().slice(0, 120) || 'Póster', artist: (v.artist || '').trim().slice(0, 80) || null, year: y >= 1800 && y <= 2100 ? y : null, kind: 'original', source: 'commons', image_url: x.url, thumb_url: x.thumb, credit: x.credit || null, license: x.license || null, source_url: x.src, created_by: revMe().uid } });
    toast('Guardado en la galería como Original.'); rev.posters = [];
  } catch (e) { toast(e.message, true, 6000); }
}
async function revSaveCommunity(blob, meta, source) {
  const extra = { designer: String(meta.designer || '').trim().slice(0, 80) || null, challenge_id: meta.challenge_id || null };
  const id = revUid(), file = new File([blob], 'p.jpg', { type: blob.type || 'image/jpeg' });
  const big = await revResize(file, 1800, 0.88), th = await revResize(file, 520, 0.8);
  const image_url = await revUpload(big.blob, `p-${id}.jpg`), thumb_url = await revUpload(th.blob, `p-${id}-t.jpg`);
  const y = parseInt(meta.year, 10);
  try {
    const r = await revFetch('/rest/v1/posters', { method: 'POST', prefer: 'return=representation', body: { title: String(meta.title || 'Póster').trim().slice(0, 120) || 'Póster', artist: String(meta.artist || '').trim().slice(0, 80) || null, year: y >= 1800 && y <= 2100 ? y : null, kind: 'comunidad', source, image_url, thumb_url, ...extra, created_by: revMe().uid } });
    return { ...r[0], profiles: { name: (rev.me && rev.me.name) || (revMe() || {}).name || 'Tú' } };
  } catch (e) { await revUnupload(image_url); await revUnupload(thumb_url); throw e; }
}
async function revUploadPoster(ch) {
  if (!(await revNeed())) return;
  const v = await uiDialog({ title: ch ? 'Participar en el reto' : 'Subir un póster', text: 'Sube solo diseños tuyos o que tengas permiso de compartir. Al subirlo lo confirmas.', fields: [{ key: 'file', label: 'Imagen', type: 'file', accept: 'image/*' }, { key: 'title', label: 'Título' }, { key: 'artist', label: 'Banda o artista' }, { key: 'designer', label: 'Arte de (quién lo diseñó)', value: (rev.me && rev.me.name) || '' }, { key: 'year', label: 'Año', mode: 'numeric', value: new Date().getFullYear() }], ok: 'Subir' });
  if (v && ch) v.challenge_id = ch;
  if (!v) return; if (!v.file) return toast('Elige una imagen.', true); if (!(v.title || '').trim()) return toast('Escribe un título.', true);
  try { toast('Subiendo póster…', false, 20000); const p = await revSaveCommunity(v.file, v, 'subido'); toast(ch ? '¡Ya participas en el reto!' : 'Póster publicado en la comunidad.'); rev.posters = []; if (ch) return revChallenges(); revPosterOpen(p); }
  catch (e) { toast(e.message, true, 6000); }
}

/* --- Paquete 1 · Protección: denuncias y panel de administrador --- */
const REV_CAT = { spam: 'Spam o publicidad', ofensivo: 'Ofensivo o acoso', copyright: 'Copia sin permiso', falso: 'Información falsa', otro: 'Otro' };
const REV_TYPE = { post: 'Artículo', comment: 'Comentario', poster: 'Póster', profile: 'Perfil', artist: 'Artista', poll: 'Debate', event: 'Evento', vault: 'Concierto de la Bóveda', vcomment: 'Comentario de la Bóveda', wall: 'Nota de un muro', ccomment: 'Comentario del club', list: 'Lista' };
async function revReport(type, id) {
  if (!REV_TYPE[type] || !UUID.test(id) || !(await revNeed())) return;
  const cat = await uiDialog({ title: 'Denunciar', text: '¿Qué pasa con esto? Solo lo verá el administrador.', options: Object.entries(REV_CAT).map(([value, label]) => ({ label, value })), cancel: 'Cancelar' });
  if (!cat) return;
  const v = await uiDialog({ title: 'Detalles (opcional)', fields: [{ key: 'r', label: 'Cuéntanos más', type: 'textarea', rows: 3, placeholder: 'Ej: copia un póster de otro sitio' }], ok: 'Enviar denuncia' });
  if (!v) return;
  try {
    await revFetch('/rest/v1/reports', { method: 'POST', prefer: 'return=minimal', body: { target_type: type, target_id: id, category: cat, reason: (v.r || '').trim().slice(0, 300) || null, reporter: revMe().uid } });
    toast('Gracias. El administrador revisará la denuncia.');
  } catch (e) { toast(/ya existe/i.test(e.message) ? 'Ya habías denunciado esto.' : e.message, true, 6000); }
}
function revAdmin(tab) {
  if (!revIsAdmin()) return revSection('art');
  if (rev.view !== 'admin') rev.stack = [];
  rev.sec = 'admin'; rev.view = 'admin'; rev.admTab = tab || 'reports'; revBar(); ++rev.req;
  const t = (v, n) => `<button type="button" class="rv-tab${rev.admTab === v ? ' on' : ''}" data-act="adm" data-v="${v}">${n}</button>`;
  revBody().innerHTML = `<div class="rv-tabs">${t('reports', 'Denuncias')}${t('users', 'Usuarios')}${t('vault', 'Bóveda')}${t('stats', 'Estadísticas')}${t('settings', 'Ajustes')}</div><div id="rvAdm"><p class="hint">Cargando…</p></div>`;
  return ({ reports: revAdmReports, users: revAdmUsers, vault: revAdmVault, stats: revAdmStats, settings: revAdmSettings }[rev.admTab] || revAdmReports)();
}
const revAdmBox = () => $('rvAdm');
const revAdmErr = e => { if (revAdmBox()) revAdmBox().innerHTML = `<p class="hint">${esc(/actualizar la base/.test(e.message) ? 'Falta actualizar la base de datos: ejecuta revista_supabase_v3.sql en Supabase (SQL Editor).' : e.message)}</p>`; };
async function revAdmReports() {
  const my = rev.req;
  try {
    const a = await revFetch('/rest/v1/reports?select=id,target_type,target_id,category,reason,created_at,profiles!reporter(name)&status=eq.abierta&order=created_at.desc&limit=200');
    const g = new Map(); a.forEach(r => { const k = r.target_type + ':' + r.target_id; if (!g.has(k)) g.set(k, { type: r.target_type, id: r.target_id, list: [] }); g.get(k).list.push(r); });
    const ids = t => [...g.values()].filter(x => x.type === t).map(x => x.id).filter(i => UUID.test(i));
    const look = {}, get = async (t, path, sel, fn) => { const i = ids(t); if (!i.length) return; (await revFetch(`/rest/v1/${path}?select=${sel}&id=in.(${i.join(',')})`)).forEach(r => { look[t + ':' + r.id] = fn(r); }); };
    await Promise.allSettled([
      get('post', 'posts', 'id,title', r => r.title), get('comment', 'comments', 'id,body', r => r.body), get('poster', 'posters', 'id,title,artist', r => r.title + (r.artist ? ' · ' + r.artist : '')),
      get('profile', 'profiles', 'id,name', r => r.name), get('artist', 'artists', 'id,name', r => r.name),
      get('poll', 'polls', 'id,question', r => r.question), get('event', 'events', 'id,title', r => r.title), get('vault', 'vault', 'id,title', r => r.title), get('vcomment', 'vault_comments', 'id,body', r => r.body), get('wall', 'wall_posts', 'id,body', r => r.body), get('ccomment', 'club_comments', 'id,body', r => r.body), get('list', 'song_lists', 'id,title', r => r.title)]);
    if (my !== rev.req || !revAdmBox()) return;
    revAdmBox().innerHTML = g.size ? [...g.values()].map(x => {
      const what = look[x.type + ':' + x.id], gone = what === undefined;
      return `<div class="rv-rep"><p class="rv-m"><b>${REV_TYPE[x.type]}</b> · ${x.list.length} ${x.list.length === 1 ? 'denuncia' : 'denuncias'}${gone ? ' · ya no existe' : ''}</p>
<p class="rv-rep-t">${esc(String(what || '(eliminado)').slice(0, 160))}</p>
<ul>${x.list.map(r => `<li><b>${esc(REV_CAT[r.category] || r.category)}</b>${r.reason ? ': ' + esc(r.reason) : ''} <small>— ${esc(revName(r))}, ${revDate(r.created_at)}</small></li>`).join('')}</ul>
<div class="rv-pcb">${gone ? '' : `<button type="button" class="alt" data-act="admview" data-type="${x.type}" data-id="${esc(x.id)}">Ver</button>`}${gone ? '' : x.type === 'profile' ? `<button type="button" class="alt" data-act="ban" data-id="${esc(x.id)}" data-v="1">Suspender cuenta</button>` : `<button type="button" class="alt" data-act="admdel" data-type="${x.type}" data-id="${esc(x.id)}">Eliminar contenido</button>`}<button type="button" class="alt" data-act="admclose" data-type="${x.type}" data-id="${esc(x.id)}" data-v="${gone ? 'resuelta' : 'descartada'}">${gone ? 'Cerrar' : 'Descartar'}</button></div></div>`;
    }).join('') : '<p class="hint">No hay denuncias pendientes. 🎉</p>';
  } catch (e) { revAdmErr(e); }
}
async function revAdmView(type, id) {
  if (type === 'post') return revOpen(id);
  if (type === 'poster') return revPosterOpen(id);
  if (type === 'profile') return revProfile(id);
  if (type === 'artist') return revArtist(id);
  if (type === 'vault') return revVaultOpen(id);
  if (type === 'list') return lsOpen(id);
  if (type === 'ccomment') { const c = (await revFetch(`/rest/v1/club_comments?select=club_id&id=eq.${id}`))[0]; if (c) return clubView(c.club_id); return toast('Ese comentario ya no existe.'); }
  if (type === 'wall') { const w = (await revFetch(`/rest/v1/wall_posts?select=owner_id&id=eq.${id}`))[0]; if (w) { rev.profTab = 'wall'; return revProfile(w.owner_id); } return toast('Esa nota ya no existe.'); }
  if (type === 'event') return revAgenda('list');
  if (type === 'poll') { rev.sec = 'art'; rev.view = 'feed'; rev.tab = 'polls'; rev.stack = []; revBar(); return revFeed(true); }
  if (type === 'vcomment') { const c = (await revFetch(`/rest/v1/vault_comments?select=vault_id&id=eq.${id}`))[0]; if (c) return revVaultOpen(c.vault_id); return toast('Ese comentario ya no existe.'); }
  if (type === 'comment') { const c = (await revFetch(`/rest/v1/comments?select=post_id&id=eq.${id}`))[0]; if (c) return revOpen(c.post_id); toast('Ese comentario ya no existe.'); }
}
async function revAdmDelete(type, id) {
  const tbl = { post: 'posts', comment: 'comments', poster: 'posters', artist: 'artists', poll: 'polls', event: 'events', vault: 'vault', vcomment: 'vault_comments', wall: 'wall_posts', ccomment: 'club_comments', list: 'song_lists' }[type]; if (!tbl || !UUID.test(id)) return;
  if (!(await uiConfirm(`¿Eliminar este ${REV_TYPE[type].toLowerCase()}?`, 'Se borra para todos y la denuncia queda resuelta.', 'Eliminar'))) return;
  let pic = null;
  if (type === 'poster') { try { pic = (await revFetch(`/rest/v1/posters?select=source,image_url,thumb_url&id=eq.${id}`))[0]; } catch {} }
  if (type === 'post') { try { pic = { source: 'post', image_url: ((await revFetch(`/rest/v1/posts?select=cover&id=eq.${id}`))[0] || {}).cover }; } catch {} }
  await revFetch(`/rest/v1/${tbl}?id=eq.${id}`, { method: 'DELETE' });
  if (pic && pic.source === 'post') revUnPair(pic.image_url);
  else if (pic && pic.source !== 'commons') { revUnupload(pic.image_url); revUnupload(pic.thumb_url); }
  rev.posts = []; rev.posters = []; rev.artists = null;
  await revAdmClose(type, id, 'resuelta', true); toast('Contenido eliminado.');
}
async function revAdmClose(type, id, status, quiet) {
  await revFetch(`/rest/v1/reports?target_type=eq.${type}&target_id=eq.${id}&status=eq.abierta`, { method: 'PATCH', body: { status: status === 'resuelta' ? 'resuelta' : 'descartada' }, prefer: 'return=minimal' });
  if (!quiet) toast('Denuncia cerrada.');
  return revAdmin('reports');
}
async function revAdmUsers() {
  const my = rev.req, box = revAdmBox(); if (!box) return;
  if (!$('rvUSearch')) box.innerHTML = `<input id="rvUSearch" class="rv-search" type="search" placeholder="Buscar usuario por nombre…" value="${esc(rev.uQ || '')}" autocomplete="off"><div id="rvUList"><p class="hint">Cargando…</p></div>`;
  try {
    const t = revPat(rev.uQ || '');
    const a = await revFetch(`/rest/v1/profiles?select=id,name,avatar,role,banned,created_at&order=created_at.desc&limit=60${t ? `&name=ilike.${encodeURIComponent('*' + t + '*')}` : ''}`);
    if (my !== rev.req || !$('rvUList')) return;
    rev.admUsers = a;   // para mostrar el nombre actual al cambiarlo
    const me = revMe();
    $('rvUList').innerHTML = a.map(u => `<div class="rv-urow">${revAv(u, 40)}<div><b>${esc(u.name)}</b><small>${u.role === 'admin' ? 'Admin' : u.role === 'editor' ? 'Editor' : 'Usuario'}${u.banned ? ' · <span class="rv-bad">suspendida</span>' : ''} · desde ${revDate(u.created_at, { month: 'short', year: 'numeric' })}</small></div>
<div class="rv-pcb"><button type="button" class="alt" data-act="admuser" data-id="${esc(u.id)}">Perfil</button>${u.role !== 'admin' && (!me || u.id !== me.uid) ? `<button type="button" class="alt" data-act="aname" data-id="${esc(u.id)}">Nombre</button><button type="button" class="alt" data-act="ban" data-id="${esc(u.id)}" data-v="${u.banned ? 0 : 1}">${u.banned ? 'Reactivar' : 'Suspender'}</button>` : ''}</div></div>`).join('') || '<p class="hint">Sin resultados.</p>';
  } catch (e) { revAdmErr(e); }
}
async function revAdmStats() {
  const my = rev.req;
  try {
    const old = encodeURIComponent(new Date(Date.now() - 30 * 864e5).toISOString());
    const [nU, nP, nC, nPo, nA, nR, recent, top] = await Promise.all([
      revFetch('/rest/v1/profiles?select=id&limit=1', { count: 1 }), revFetch('/rest/v1/posts?select=id&limit=1', { count: 1 }), revFetch('/rest/v1/comments?select=id&limit=1', { count: 1 }),
      revFetch('/rest/v1/posters?select=id&limit=1', { count: 1 }), revFetch('/rest/v1/artists?select=id&limit=1', { count: 1 }), revFetch('/rest/v1/reports?select=id&status=eq.abierta&limit=1', { count: 1 }),
      revFetch(`/rest/v1/posts?select=author_id,profiles!author_id(name)&created_at=gte.${old}&limit=1000`),
      revFetch('/rest/v1/posts?select=id,title,created_at,likes(count),comments(count)&order=created_at.desc&limit=200')]);
    const reads = await revFetch('/rest/v1/posts?select=id,title,views&views=gt.0&order=views.desc&limit=5').catch(() => []);
    if (my !== rev.req || !revAdmBox()) return;
    const by = new Map(); recent.forEach(p => { const k = p.author_id; by.set(k, { name: revName(p), n: ((by.get(k) || {}).n || 0) + 1, id: k }); });
    const authors = [...by.values()].sort((a, b) => b.n - a.n).slice(0, 5);
    const best = top.map(p => ({ ...p, s: revCnt(p.likes) * 2 + revCnt(p.comments) })).sort((a, b) => b.s - a.s).slice(0, 5);
    const k = (n, l) => `<div class="rv-kpi"><b>${n}</b><span>${l}</span></div>`;
    revAdmBox().innerHTML = `<div class="rv-kpis">${k(nU, 'usuarios')}${k(nP, 'artículos')}${k(nC, 'comentarios')}${k(nPo, 'pósters')}${k(nA, 'artistas')}${k(nR, 'denuncias abiertas')}</div>
<h4 class="rv-h">Autores más activos (30 días)</h4>${authors.length ? authors.map(a => `<div class="rv-urow rv-click" data-act="admuser" data-id="${esc(a.id)}" tabindex="0"><div><b>${esc(a.name)}</b><small>${a.n} ${a.n === 1 ? 'artículo' : 'artículos'}</small></div></div>`).join('') : '<p class="hint">Nadie ha publicado este mes.</p>'}
<h4 class="rv-h">Artículos con más reacciones</h4>${best.length ? best.map(p => `<div class="rv-urow rv-click" data-act="open" data-id="${esc(p.id)}" tabindex="0"><div><b>${esc(p.title)}</b><small>${revCnt(p.likes)} me gusta · ${revCnt(p.comments)} comentarios · ${revDate(p.created_at)}</small></div></div>`).join('') : '<p class="hint">Aún no hay artículos.</p>'}
${reads.length ? `<h4 class="rv-h">Más leídos</h4>${reads.map(p => `<div class="rv-urow rv-click" data-act="open" data-id="${esc(p.id)}" tabindex="0"><div><b>${esc(p.title)}</b><small>${p.views} lecturas</small></div></div>`).join('')}` : ''}`;
  } catch (e) { revAdmErr(e); }
}
async function revAdmSettings() {
  const my = rev.req;
  try {
    const s = (await revFetch('/rest/v1/rr_settings?select=invite_only,invite_code&id=eq.1'))[0] || {};
    if (my !== rev.req || !revAdmBox()) return;
    revAdmBox().innerHTML = `<div class="rv-form"><label class="chk"><input type="checkbox" id="rvInvOn"${s.invite_only ? ' checked' : ''}> Solo con invitación (las cuentas nuevas necesitan un código)</label>
<label for="rvInvCode">Código de invitación</label><input id="rvInvCode" maxlength="40" autocomplete="off" value="${esc(s.invite_code || '')}" placeholder="Ej: rock2026">
<p class="hint">Compártelo solo con quien quieras dejar entrar. Las cuentas que ya existen no se ven afectadas. Cámbialo cuando quieras para cerrar la puerta.</p>
<div class="row"><button type="button" data-act="admsave">Guardar</button></div>
<h4 class="rv-h">Copia de seguridad</h4><p class="hint">Descarga en un archivo todo el contenido público de la revista (perfiles, artículos, comentarios, pósters, artistas, agenda…). Guárdalo en un lugar seguro.</p><div class="row"><button type="button" class="alt" data-act="backup">Descargar copia de seguridad</button></div>
<h4 class="rv-h">Límites automáticos contra spam</h4><p class="hint">Por persona y por hora: 10 artículos, 30 comentarios, 15 pósters y 20 denuncias. Tú no tienes límite. Las cuentas suspendidas pueden leer, pero no publicar, comentar, dar me gusta ni seguir.</p></div>`;
  } catch (e) { revAdmErr(e); }
}
async function revAdmSaveSettings() {
  const on = $('rvInvOn').checked, code = $('rvInvCode').value.trim();
  if (on && code.length < 4) return toast('El código debe tener al menos 4 caracteres.', true);
  await revFetch('/rest/v1/rr_settings?id=eq.1', { method: 'PATCH', body: { invite_only: on, invite_code: code || null }, prefer: 'return=minimal' });
  toast(on ? 'Listo: la revista ahora es solo con invitación.' : 'Listo: cualquiera puede crear una cuenta.');
}

/* --- Paquete 2 · Enganche: notificaciones, respuestas, recomendar, insignias, debates, retos, normas --- */
const REV_RULES = [
  ['Respeto', 'Critica la música, no a las personas. Nada de insultos, acoso, odio ni amenazas.'],
  ['Arte propio', 'Sube solo pósters e imágenes que hiciste tú o que tienes permiso de compartir. Da crédito a quien los diseñó.'],
  ['Fuentes', 'Si citas algo de otro sitio, enlázalo. No copies artículos completos de otras personas.'],
  ['Nada de spam', 'Sin publicidad repetida, cadenas ni enlaces engañosos.'],
  ['Contenido apto', 'Nada sexual explícito, violento o ilegal. La revista la leen personas de todas las edades.'],
  ['Moderación', 'El administrador puede quitar contenido o suspender cuentas que rompan estas normas. Si ves algo así, usa «Denunciar».']];
function revRules() {
  revPush(); ++rev.req; rev.view = 'rules'; revBar();
  revBody().innerHTML = revHead('Volver', 'Normas de la comunidad') + `<div class="rv-rules">${REV_RULES.map(([t, d], i) => `<div><span>${i + 1}</span><p><b>${t}.</b> ${d}</p></div>`).join('')}</div><p class="hint">Privacidad: guardamos tu correo solo para tu cuenta. Tu nombre, foto, biografía y lo que publicas son públicos. Puedes pedir al administrador que borre tu cuenta.</p><p class="hint"><a href="legal.html" target="_blank" rel="noopener">Aviso legal, descargo de responsabilidad y privacidad ↗</a></p>`;
}
const revRulesText = () => REV_RULES.map(([t, d]) => `${t}: ${d}`).join(' ') + ' Al crear tu cuenta aceptas también el aviso legal y de privacidad (lo encuentras en Normas y en la Guía de uso). Debes tener 13 años o más.';
/* Notificaciones */
async function revNotifCount() {
  const me = revMe(); if (!me || rev.v1) return;
  if (!rev.v8off && Date.now() - (rev.relT || 0) > 10 * 60e3) { rev.relT = Date.now(); try { await revFetch('/rest/v1/rpc/rr_vault_release', { method: 'POST', body: {} }); } catch (e) { if (/rr_vault_release|schema cache|function/i.test(e.message + (e.raw || ''))) rev.v8off = true; } }
  try { rev.unread = await revFetch(`/rest/v1/notifications?select=id&user_id=eq.${me.uid}&read=eq.false&limit=1`, { count: 1 }); } catch { rev.unread = 0; }
  const b = document.querySelector('#revAcct .rv-bell b'); if (b) { b.textContent = rev.unread > 9 ? '9+' : rev.unread; b.hidden = !rev.unread; }
}
const REV_NK = { follow: 'empezó a seguirte', like: 'le dio me gusta a tu artículo', comment: 'comentó tu artículo', reply: 'respondió tu comentario', recommend: 'recomendó tu artículo', wall: 'escribió en tu muro', mention: 'te mencionó en un muro', wreply: 'respondió tu nota' };
async function revNotifs(noPush) {
  if (!(await revNeed())) return;
  if (!noPush) revPush(); rev.view = 'notifs'; revBar(); const my = ++rev.req, me = revMe();
  revBody().innerHTML = revHead('Volver', 'Notificaciones') + '<div id="rvNList"><p class="hint">Cargando…</p></div>';
  try {
    const base = `/rest/v1/notifications?select=id,kind,read,created_at,post_id,actor_id,actor:profiles!notif_actor_fk(name,avatar),posts(title)`, tail = `&user_id=eq.${me.uid}&order=created_at.desc&limit=60`;
    const a = await revFetch(base + ',wall_owner,vault_id,vault(title)' + tail).catch(() => revFetch(base + tail));
    if (my !== rev.req) return;
    $('rvNList').innerHTML = a.map(n => {
      if (n.kind === 'vault' || n.kind === 'vault_live') return `<div class="rv-urow rv-click${n.read ? '' : ' rv-new'}" data-act="bvopen" data-id="${esc(n.vault_id || '')}" tabindex="0"><span class="rv-av rv-av0 nv-nfi" style="width:40px;height:40px">${ICO('film')}</span><div><span>${n.kind === 'vault' ? 'Ya está en cartelera' : 'Empieza pronto la función en vivo de'} <b>«${esc((n.vault && n.vault.title) || 'un concierto')}»</b></span><small>${revAgo(n.created_at)}</small></div></div>`;
      const wallK = ['wall', 'mention', 'wreply'].includes(n.kind), tgt = n.post_id || (wallK ? (n.wall_owner || me.uid) : n.actor_id);
      return `<div class="rv-urow rv-click${n.read ? '' : ' rv-new'}" data-act="${n.post_id ? 'open' : 'profile'}" data-id="${esc(tgt)}" tabindex="0">${revAv(n.actor, 40)}<div><span><b>${esc((n.actor && n.actor.name) || 'Alguien')}</b> ${REV_NK[n.kind] || ''}${n.posts && n.posts.title ? ` «${esc(n.posts.title)}»` : ''}</span><small>${revAgo(n.created_at)}</small></div></div>`;
    }).join('') || '<p class="hint">Aún no tienes notificaciones. Cuando alguien te siga, comente o te dé me gusta, aparecerá aquí.</p>';
    if (a.some(n => !n.read)) { await revFetch(`/rest/v1/notifications?user_id=eq.${me.uid}&read=eq.false`, { method: 'PATCH', body: { read: true }, prefer: 'return=minimal' }); rev.unread = 0; revBar(); }
  } catch (e) { if ($('rvNList')) $('rvNList').innerHTML = `<p class="hint">${esc(/actualizar la base/.test(e.message) ? V4_MSG : e.message)}</p>`; }
}
const V4_MSG = 'Falta actualizar la base de datos: ejecuta revista_supabase_v4.sql en Supabase (SQL Editor).';
const revAgo = s => { const m = Math.round((Date.now() - new Date(s)) / 60000); return m < 1 ? 'ahora' : m < 60 ? `hace ${m} min` : m < 1440 ? `hace ${Math.round(m / 60)} h` : m < 10080 ? `hace ${Math.round(m / 1440)} d` : revDate(s); };
setInterval(() => { try { if ($('view-revista').classList.contains('active') && !document.hidden) revNotifCount(); } catch {} }, 90000);
/* Recomendar */
async function revRecommend() {
  const p = rev.post; if (!p || !(await revNeed())) return;
  const me = revMe();
  if (rev.recs.has(p.id)) {
    await revFetch(`/rest/v1/recommends?post_id=eq.${p.id}&user_id=eq.${me.uid}`, { method: 'DELETE' });
    rev.recs.delete(p.id); p.recommends = [{ count: Math.max(0, revCnt(p.recommends) - 1) }]; toast('Ya no lo recomiendas.'); return revDrawPost();
  }
  const v = await uiDialog({ title: 'Recomendar en tu perfil', text: 'Quienes te siguen lo verán en «Siguiendo».', fields: [{ key: 'note', label: 'Una nota (opcional)', type: 'textarea', rows: 2, placeholder: 'Por qué vale la pena leerlo' }], ok: 'Recomendar' });
  if (!v) return;
  await revFetch('/rest/v1/recommends', { method: 'POST', prefer: 'return=minimal', body: { post_id: p.id, user_id: me.uid, note: (v.note || '').trim().slice(0, 280) || null } });
  rev.recs.add(p.id); p.recommends = [{ count: revCnt(p.recommends) + 1 }]; toast('Recomendado en tu perfil.'); revDrawPost();
}
/* Insignias (se calculan a partir de lo que cada quien hizo) */
async function revBadges(p) {
  const el = $('rvBadges'); if (!el || rev.v1) return;
  const id = p.id, c = (t, f) => revFetch(`/rest/v1/${t}?select=${t === 'recommends' || t === 'likes' ? 'post_id' : 'id'}&${f}=eq.${id}&limit=1`, { count: 1 }).catch(() => 0);
  const [np, npo, nc, nr] = await Promise.all([c('posts', 'author_id'), c('posters', 'created_by'), c('comments', 'author_id'), c('recommends', 'user_id')]);
  let mg = 0; try { mg = (await revFetch(`/rest/v1/artist_managers?select=artist_id,artists(verified)&user_id=eq.${id}`)).filter(x => x.artists && x.artists.verified).length; } catch {}
  const b = [];
  if (mg) b.push(['✓', 'Banda verificada', 'Gestiona el perfil oficial de una banda']);
  if (p.role === 'admin') b.push(['★', 'Fundador de la revista', 'Administra Radio de ruta']); else if (p.role === 'editor') b.push(['✎', 'Editor', 'Cuida los perfiles de artistas']);
  if (new Date(p.created_at) < new Date('2027-01-01')) b.push(['◆', 'Pionero', 'Llegó en el primer año']);
  if (np >= 1) b.push(['✍', 'Primer artículo', 'Publicó su primer artículo']);
  if (np >= 10) b.push(['🖋', 'Pluma de oro', '10 artículos o más']);
  if (npo >= 10) b.push(['▣', 'Coleccionista', '10 pósters o más']);
  if (nc >= 25) b.push(['💬', 'Conversador', '25 comentarios o más']);
  if (nr >= 20) b.push(['♪', 'Oído fino', 'Recomendó 20 artículos o más']);
  el.innerHTML = b.map(([i, t, d]) => `<span class="rv-badge2" title="${esc(d)}"><i>${i}</i>${esc(t)}</span>`).join('');
}
/* Debates (encuestas) */
async function revPolls() {
  rev.sec = 'art'; rev.view = 'feed'; rev.tab = 'polls'; revBar(); const my = ++rev.req, box = revBody();
  box.innerHTML = '<p class="hint">Cargando…</p>';
  try {
    const a = await revFetch('/rest/v1/polls?select=id,question,options,closes_at,created_at,author_id,profiles!author_id(name,avatar)&order=created_at.desc&limit=30');
    const ids = a.map(p => p.id).filter(i => UUID.test(i)), me = revMe();
    const [res, mine] = await Promise.all([ids.length ? revFetch('/rest/v1/rpc/rr_polls_results', { method: 'POST', body: { ids } }) : [], me && ids.length ? revFetch(`/rest/v1/poll_votes?select=poll_id,option&user_id=eq.${me.uid}`) : []]);
    if (my !== rev.req) return;
    const R = new Map(res.map(x => [x.poll_id, x.counts || []])), V = new Map(mine.map(x => [x.poll_id, x.option]));
    rev.polls = a;
    box.innerHTML = '<p class="hint">Debates de la comunidad. Tu voto es privado: solo se ven los totales.</p>' + (a.length ? a.map(p => {
      const cs = R.get(p.id) || [], tot = cs.reduce((x, y) => x + y, 0), my = V.get(p.id), closed = p.closes_at && new Date(p.closes_at) < new Date(), show = my !== undefined || closed;
      const opts = Array.isArray(p.options) ? p.options : [];
      return `<div class="rv-poll"><p class="rv-au">${revAv(p.profiles, 22)}<a href="#" data-act="profile" data-id="${esc(p.author_id)}">${esc(revName(p))}</a> · ${revAgo(p.created_at)}${closed ? ' · cerrado' : p.closes_at ? ' · cierra ' + revDate(p.closes_at) : ''}</p><h3>${esc(p.question)}</h3>
${opts.map((o, i) => { const n = cs[i] || 0, pc = tot ? Math.round(n * 100 / tot) : 0; return show ? `<button type="button" class="rv-opt${my === i ? ' on' : ''}" data-act="vote" data-id="${esc(p.id)}" data-i="${i}"${closed ? ' disabled' : ''}><span style="width:${pc}%"></span><b>${esc(String(o))}</b><em>${pc}%</em></button>` : `<button type="button" class="rv-opt" data-act="vote" data-id="${esc(p.id)}" data-i="${i}"><b>${esc(String(o))}</b></button>`; }).join('')}
<p class="rv-m">${tot} ${tot === 1 ? 'voto' : 'votos'}${my !== undefined && !closed ? ' · toca otra opción para cambiar tu voto' : ''}${me && (me.uid === p.author_id || revIsAdmin()) ? ` · <button type="button" class="rv-x" data-act="delpoll" data-id="${esc(p.id)}">Borrar</button>` : ''}${me && me.uid !== p.author_id ? ` · <button type="button" class="rv-x" data-act="report" data-type="poll" data-id="${esc(p.id)}">Denunciar</button>` : ''}</p></div>`;
    }).join('') : '<p class="hint">Todavía no hay debates. Abre el primero con el botón +.</p>');
  } catch (e) { if (my === rev.req) box.innerHTML = `<p class="hint">${esc(/actualizar la base/.test(e.message) ? V4_MSG : e.message)}</p>`; }
}
async function revVote(id, i) {
  if (!(await revNeed())) return; const me = revMe();
  const had = await revFetch(`/rest/v1/poll_votes?select=option&poll_id=eq.${id}&user_id=eq.${me.uid}`);
  if (had.length) await revFetch(`/rest/v1/poll_votes?poll_id=eq.${id}&user_id=eq.${me.uid}`, { method: 'PATCH', body: { option: i }, prefer: 'return=minimal' });
  else await revFetch('/rest/v1/poll_votes', { method: 'POST', body: { poll_id: id, user_id: me.uid, option: i }, prefer: 'return=minimal' });
  return revPolls();
}
async function revNewPoll() {
  if (!(await revNeed())) return;
  const v = await uiDialog({ title: 'Nuevo debate', fields: [{ key: 'q', label: 'Pregunta', placeholder: '¿Mejor disco de 1991?' }, { key: 'o', label: 'Opciones (una por línea, de 2 a 6)', type: 'textarea', rows: 5, placeholder: 'Nevermind\nTen\nLoveless' }, { key: 'd', label: 'Cerrar en cuántos días (vacío = nunca)', mode: 'numeric' }], ok: 'Publicar' });
  if (!v) return;
  const q = (v.q || '').trim(), o = [...new Set((v.o || '').split('\n').map(x => x.trim().slice(0, 80)).filter(Boolean))].slice(0, 6), d = parseInt(v.d, 10);
  if (q.length < 5) return toast('Escribe una pregunta (mínimo 5 letras).', true);
  if (o.length < 2) return toast('Pon al menos 2 opciones distintas.', true);
  await revFetch('/rest/v1/polls', { method: 'POST', prefer: 'return=minimal', body: { author_id: revMe().uid, question: q.slice(0, 200), options: o, closes_at: d > 0 ? new Date(Date.now() + d * 864e5).toISOString() : null } });
  toast('Debate publicado.'); revPolls();
}
/* Retos mensuales de pósters */
const revToday10 = (d = new Date()) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`; // fecha local, no UTC
async function revChallenges() {
  rev.sec = 'posters'; rev.view = 'posters'; rev.pFilter = 'retos'; const my = ++rev.req, el = $('rvPGrid'); if (!el) return revPosters(false);
  el.innerHTML = '<p class="hint">Cargando…</p>';
  try {
    const today = revToday10(), me = revMe();
    const cs = await revFetch('/rest/v1/challenges?select=*&order=ends_on.desc&limit=12');
    if (my !== rev.req) return;
    const cur = cs.filter(c => c.ends_on >= today && c.starts_on <= today), past = cs.filter(c => c.ends_on < today).slice(0, 4);
    const block = async (c, live) => {
      const [ps, res, mv] = await Promise.all([revFetch(`/rest/v1/posters?select=${REV_PSEL()}&challenge_id=eq.${c.id}&order=created_at.asc&limit=60`), revFetch('/rest/v1/rpc/rr_challenge_results', { method: 'POST', body: { ch: c.id } }), me ? revFetch(`/rest/v1/challenge_votes?select=poster_id&challenge_id=eq.${c.id}&user_id=eq.${me.uid}`) : []]);
      const V = new Map(res.map(x => [x.poster_id, x.votes])), mine = (mv[0] || {}).poster_id;
      ps.forEach(p => { p.votes = V.get(p.id) || 0; }); const order = live ? ps : ps.slice().sort((a, b) => b.votes - a.votes);
      ps.forEach(p => { if (!rev.posters.some(x => x.id === p.id)) rev.posters.push(p); });
      const st = revSt({ th: c.aesthetic || 'asfalto', ...(REV_PRE[c.aesthetic] || {}), cv: 'm' });
      return `<section class="rv-chal rv-st" ${revAttrs(st)}><p class="rv-lab">${live ? 'Reto en curso' : 'Reto terminado'} · ${revDate(c.starts_on, { day: 'numeric', month: 'short' })} – ${revDate(c.ends_on)}</p><h2>${esc(c.title)}</h2>${c.description ? `<p class="rv-bio">${esc(c.description)}</p>` : ''}
${c.aesthetic && REV_TH[c.aesthetic] ? `<p class="rv-m">Estética: ${esc(REV_TH[c.aesthetic])}</p>` : ''}${live ? `<div class="rv-pbtns"><button type="button" data-act="chjoin" data-id="${esc(c.id)}">Participar con mi póster</button>${revIsAdmin() ? `<button type="button" class="alt" data-act="chdel" data-id="${esc(c.id)}">Borrar reto</button>` : ''}</div>` : ''}</section>
${order.length ? `<div class="rv-pgrid">${order.map((p, i) => `<figure class="rv-pcard"><img src="${esc(revUrl(p.thumb_url) || revUrl(p.image_url))}" alt="" loading="lazy" decoding="async" data-act="poster" data-id="${esc(p.id)}"><figcaption>${!live && i === 0 && p.votes ? '<span class="rv-badge og">Ganador</span>' : ''}<b>${esc(p.title)}</b><small>Arte de ${esc(p.designer || revName(p))}</small><small>${p.votes} ${p.votes === 1 ? 'voto' : 'votos'}</small>${live && (!me || me.uid !== p.created_by) ? `<button type="button" class="${mine === p.id ? '' : 'alt'}" data-act="chvote" data-ch="${esc(c.id)}" data-id="${esc(p.id)}">${mine === p.id ? 'Tu voto' : 'Votar'}</button>` : ''}</figcaption></figure>`).join('')}</div>` : `<p class="hint">${live ? 'Aún no hay participantes. ¡Sé el primero!' : 'Nadie participó.'}</p>`}`;
    };
    let h = revIsAdmin() ? '<div class="row"><button type="button" class="alt" data-act="chnew">+ Nuevo reto</button></div>' : '';
    h += cur.length ? (await Promise.all(cur.map(c => block(c, true)))).join('') : '<p class="hint">No hay un reto activo ahora mismo.</p>';
    if (past.length) h += '<h4 class="rv-h">Retos anteriores</h4>' + (await Promise.all(past.map(c => block(c, false)))).join('');
    if (my === rev.req && $('rvPGrid')) $('rvPGrid').innerHTML = h;
  } catch (e) { if (my === rev.req && $('rvPGrid')) $('rvPGrid').innerHTML = `<p class="hint">${esc(/actualizar la base/.test(e.message) ? V4_MSG : e.message)}</p>`; }
}
async function revNewChallenge() {
  if (!revIsAdmin()) return;
  const est = await uiDialog({ title: 'Nuevo reto: estética', options: [{ label: 'Libre', value: '' }, ...Object.keys(REV_TH).slice(6).map(k => ({ label: REV_TH[k], value: k }))], cancel: 'Cancelar' });
  if (est === null || est === undefined || est === false) return;
  const d = new Date(), end = revToday10(new Date(d.getFullYear(), d.getMonth() + 1, 0));
  const v = await uiDialog({ title: 'Nuevo reto', fields: [{ key: 't', label: 'Título', placeholder: 'Diseña el póster de tu banda favorita' }, { key: 'd', label: 'Descripción', type: 'textarea', rows: 3 }, { key: 'e', label: 'Termina el (AAAA-MM-DD)', value: end }], ok: 'Crear reto' });
  if (!v) return;
  if ((v.t || '').trim().length < 3) return toast('Escribe un título.', true);
  if (!/^\d{4}-\d{2}-\d{2}$/.test((v.e || '').trim())) return toast('La fecha debe ser AAAA-MM-DD.', true);
  await revFetch('/rest/v1/challenges', { method: 'POST', prefer: 'return=minimal', body: { title: v.t.trim().slice(0, 120), description: (v.d || '').trim().slice(0, 1000) || null, aesthetic: est || null, starts_on: revToday10(), ends_on: v.e.trim(), created_by: revMe().uid } });
  toast('Reto creado.'); revChallenges();
}
async function revChVote(ch, id) {
  if (!(await revNeed())) return; const me = revMe();
  const had = await revFetch(`/rest/v1/challenge_votes?select=poster_id&challenge_id=eq.${ch}&user_id=eq.${me.uid}`);
  if (had.length) await revFetch(`/rest/v1/challenge_votes?challenge_id=eq.${ch}&user_id=eq.${me.uid}`, { method: 'PATCH', body: { poster_id: id }, prefer: 'return=minimal' });
  else await revFetch('/rest/v1/challenge_votes', { method: 'POST', prefer: 'return=minimal', body: { challenge_id: ch, poster_id: id, user_id: me.uid } });
  toast('¡Voto registrado!'); revChallenges();
}

async function revMergeRecs(posts) {
  const ids = [...rev.follows].filter(i => UUID.test(i)); if (!ids.length) return posts;
  try {
    const rc = await revFetch(`/rest/v1/recommends?select=note,created_at,user_id,profiles!user_id(name),posts(${REV_CARD()})&user_id=in.(${ids.join(',')})&order=created_at.desc&limit=20`);
    const seen = new Set(posts.map(p => p.id)), me = revMe(), extra = [];
    rc.forEach(r => { if (r.posts && !seen.has(r.posts.id) && (!me || r.posts.author_id !== me.uid)) { seen.add(r.posts.id); extra.push({ ...r.posts, _rec: { name: revName(r), note: r.note }, _t: r.created_at }); } });
    return posts.concat(extra).sort((a, b) => String(b._t || b.created_at).localeCompare(String(a._t || a.created_at)));
  } catch { return posts; }
}

/* --- Paquete 3 · Música: lista del artículo, la locutora lee, reseñas, discografía, hoy en la música, agenda, bandas verificadas --- */
function revPlayList() {
  const ids = rev.ids.slice(); if (!ids.length) return toast('Ninguna canción del artículo está en tu biblioteca.');
  const set = new Set(ids); playLib(r => set.has(r.id), ids[0]);
}
let revReadTok = 0;
function revStopRead() { revReadTok++; try { speakId++; speechSynthesis.cancel(); } catch {} try { if (cloud) cloud.pause(); } catch {} rev.reading = false; }
async function revRead() {
  const p = rev.post; if (!p) return;
  if (rev.reading) { revStopRead(); return revDrawPost(); }
  if (running) return toast('Detén la radio para que la locutora te lea el artículo.', false, 5000);
  const plain = s => String(s || '').replace(/\*\*?|^#+ |^> |^- /gm, '').replace(/https?:\/\/\S+/g, '');
  const parts = [p.title + (p.subtitle ? '. ' + p.subtitle : '') + '. Por ' + revName(p) + '.', ...plain(p.body).split(/\n{2,}/).map(x => x.replace(/\n/g, ' ').trim()).filter(Boolean)];
  const tok = ++revReadTok; rev.reading = true; revDrawPost();
  try { for (const t of parts) { if (tok !== revReadTok) break; await speak(t.slice(0, 900)); } }
  finally { if (tok === revReadTok) { rev.reading = false; if (rev.view === 'post' && rev.post === p) revDrawPost(); } }
}
/* Reseñas de discos */
const revReview = r => { if (!r || typeof r !== 'object') return null; const s = Math.round(+r.score); if (!(s >= 1 && s <= 10)) return null; return { album: revClean(String(r.album || ''), 100), artist: revClean(String(r.artist || ''), 80), year: /^\d{4}$/.test(String(r.year || '')) ? +r.year : null, score: s }; };
const revReviewBox = r => `<div class="rv-review"><span class="rv-score">${r.score}<small>/10</small></span><div><b>${esc(r.album)}</b><small>${esc([r.artist, r.year].filter(Boolean).join(' · '))}</small><i style="--s:${r.score * 10}%"></i></div></div>`;
function revReviewUI(r) {
  const on = !!r; r = r || { album: '', artist: '', year: '', score: 7 };
  return `<label class="chk"><input type="checkbox" id="rvRvOn"${on ? ' checked' : ''}> Este artículo es una reseña de disco</label><div id="rvRvF"${on ? '' : ' hidden'}><div class="rv-two"><label>Disco<input id="rvRvAlbum" maxlength="100" value="${esc(r.album)}"></label><label>Artista<input id="rvRvArtist" maxlength="80" value="${esc(r.artist)}"></label></div><div class="rv-two"><label>Año<input id="rvRvYear" inputmode="numeric" maxlength="4" value="${esc(r.year || '')}"></label><label>Puntuación: <b id="rvRvS">${r.score}</b>/10<input id="rvRvScore" type="range" min="1" max="10" step="1" value="${r.score}"></label></div></div>`;
}
const revReviewRead = () => $('rvRvOn') && $('rvRvOn').checked ? revReview({ album: $('rvRvAlbum').value, artist: $('rvRvArtist').value, year: $('rvRvYear').value, score: $('rvRvScore').value }) : null;
/* Discografía (MusicBrainz) — solo si el nombre coincide exactamente */
async function revDisco(a) {
  const el = $('rvDisco'); if (!el) return;
  const key = 'rr_disco_v1_' + revNk(a.name); let d = null;
  try { const c = JSON.parse(localStorage.getItem(key) || 'null'); if (c && Date.now() - c.t < 30 * 864e5) d = c.d; } catch {}
  if (!d) {
    el.innerHTML = '<p class="hint">Buscando discografía en MusicBrainz…</p>';
    const j = await mbJson(`https://musicbrainz.org/ws/2/artist/?query=${encodeURIComponent('artist:"' + lq(a.name) + '"')}&fmt=json&limit=5`);
    const hit = ((j && j.artists) || []).find(x => x.score >= 90 && revNk(x.name) === revNk(a.name));
    if (!hit) { el.innerHTML = '<p class="hint">No encontré una coincidencia exacta en MusicBrainz.</p>'; return; }
    const g = await mbJson(`https://musicbrainz.org/ws/2/release-group?artist=${hit.id}&type=album&fmt=json&limit=100`);
    d = ((g && g['release-groups']) || []).filter(x => !(x['secondary-types'] || []).length).map(x => ({ id: x.id, t: x.title, y: (x['first-release-date'] || '').slice(0, 4) })).sort((x, y) => (x.y || '9999').localeCompare(y.y || '9999')).slice(0, 40);
    try { localStorage.setItem(key, JSON.stringify({ t: Date.now(), d })); } catch {}
  }
  if (!$('rvDisco')) return;
  $('rvDisco').innerHTML = d.length ? `<div class="rv-prow">${d.map(x => `<figure class="rv-disc"><img src="https://coverartarchive.org/release-group/${esc(x.id)}/front-250" alt="" loading="lazy" decoding="async" onerror="this.replaceWith(Object.assign(document.createElement('div'),{className:'rv-ph',textContent:'♪'}))"><figcaption><b>${esc(x.t)}</b><small>${esc(x.y || '')}</small></figcaption></figure>`).join('')}</div><p class="rv-m">Datos: MusicBrainz · portadas: Cover Art Archive</p>` : '<p class="hint">MusicBrainz no tiene álbumes de estudio para este artista.</p>';
}
async function revArtistReviews(a) {
  const el = $('rvAReviews'); if (!el) return;
  try {
    const pat = revPat(a.name); if (!pat) return;
    const r = await revFetch(`/rest/v1/posts?select=id,title,review,created_at,author_id,profiles!author_id(name)&review->>artist=ilike.${encodeURIComponent('*' + pat + '*')}&order=created_at.desc&limit=30`);
    const ok = r.map(p => ({ ...p, rv: revReview(p.review) })).filter(p => p.rv); if (!$('rvAReviews')) return;
    if (!ok.length) { el.innerHTML = '<p class="hint">Aún no hay reseñas de sus discos. Escribe una marcando «reseña de disco» en el editor.</p>'; return; }
    const avg = ok.reduce((s, p) => s + p.rv.score, 0) / ok.length;
    el.innerHTML = `<p class="rv-avg"><b>${avg.toFixed(1)}</b>/10 · promedio de la comunidad (${ok.length} ${ok.length === 1 ? 'reseña' : 'reseñas'})</p>${ok.map(p => `<div class="rv-urow rv-click" data-act="open" data-id="${esc(p.id)}" tabindex="0"><span class="rv-score sm">${p.rv.score}</span><div><b>${esc(p.rv.album)}</b><small>${esc(p.title)} · por ${esc(revName(p))}</small></div></div>`).join('')}`;
  } catch (e) { el.innerHTML = `<p class="hint">${esc(/actualizar la base/.test(e.message) ? V4_MSG : e.message)}</p>`; }
}
/* Enlaces y verificación de bandas */
const REV_LINKS = [['bandcamp', 'Bandcamp'], ['spotify', 'Spotify'], ['youtube', 'YouTube'], ['instagram', 'Instagram'], ['web', 'Sitio oficial']];
const revLinks = l => { const o = {}; if (l && typeof l === 'object') REV_LINKS.forEach(([k]) => { const v = String(l[k] || '').trim(); if (/^https:\/\/[^\s"'<>]{4,300}$/.test(v)) o[k] = v; }); return o; };
async function revManagers(a) {
  const el = $('rvMgr'); if (!el) return;
  try {
    const m = await revFetch(`/rest/v1/artist_managers?select=user_id,profiles!user_id(name)&artist_id=eq.${a.id}`); rev.aManagers = m.map(x => x.user_id);
    const me = revMe(); if (me && rev.aManagers.includes(me.uid) && !$('rvAEditBtn')) { const h = document.querySelector('#revBody .rv-head'); if (h) h.insertAdjacentHTML('beforeend', '<button type="button" class="alt" id="rvAEditBtn" data-act="editartist">Editar</button>'); }
    if (!revIsAdmin()) { el.innerHTML = m.length ? `<p class="rv-m">Perfil gestionado por: ${m.map(x => esc(revName(x))).join(', ')}</p>` : ''; return; }
    el.innerHTML = `<div class="rv-adminbox"><p class="rv-m"><b>Solo tú ves esto</b></p><div class="rv-pbtns"><button type="button" class="alt" data-act="verify" data-v="${a.verified ? 0 : 1}">${a.verified ? 'Quitar verificación' : 'Verificar banda ✓'}</button><button type="button" class="alt" data-act="addmgr">Añadir gestor</button></div>${m.length ? `<p class="rv-m">Gestores: ${m.map(x => `${esc(revName(x))} <button type="button" class="rv-x" data-act="delmgr" data-id="${esc(x.user_id)}">quitar</button>`).join(' · ')}</p>` : '<p class="hint">Un gestor es una persona de la banda que puede editar este perfil, sus enlaces y su biografía.</p>'}</div>`;
  } catch (e) { el.innerHTML = ''; }
}
async function revAddManager() {
  const v = await uiDialog({ title: 'Añadir gestor', text: 'Escribe el nombre público de su cuenta en la Revista.', fields: [{ key: 'n', label: 'Nombre' }], ok: 'Buscar' }); if (!v) return;
  const t = revPat(v.n); if (!t) return;
  const a = await revFetch(`/rest/v1/profiles?select=id,name&name=ilike.${encodeURIComponent('*' + t + '*')}&limit=8`);
  if (!a.length) return toast('No encontré esa cuenta.', true);
  const id = await uiDialog({ title: '¿Quién gestiona esta banda?', options: a.map(p => ({ label: p.name, value: p.id })), cancel: 'Cancelar' }); if (!id) return;
  await revFetch('/rest/v1/artist_managers', { method: 'POST', prefer: 'return=minimal', body: { artist_id: rev.artist.id, user_id: id } });
  toast('Gestor añadido.'); revManagers(rev.artist);
}
/* Hoy en la música (Wikipedia en español, «un día como hoy») */
async function revToday() {
  const d = new Date(), mm = String(d.getMonth() + 1).padStart(2, '0'), dd = String(d.getDate()).padStart(2, '0'), key = 'rr_hoy_v1_' + mm + dd;
  try { const c = JSON.parse(localStorage.getItem(key) || 'null'); if (c && Date.now() - c.t < 864e5) return c.e; } catch {}
  try {
    const r = await withTimeout(fetch(`https://api.wikimedia.org/feed/v1/wikipedia/es/onthisday/events/${mm}/${dd}`), 7000); if (!r.ok) return [];
    const j = await r.json(), mus = /\b(banda|grupo musical|cantante|m[uú]sico|compositor|[aá]lbum|disco|sencillo|canci[oó]n|concierto|festival|gira|rock|jazz|punk|pop|orquesta|guitarrista|baterista|bajista|rapero|reguet[oó]n|salsa|cumbia)\b/i;
    const e = (j.events || []).filter(x => mus.test(x.text || '')).slice(0, 4).map(x => ({ y: x.year, t: String(x.text || '').slice(0, 220), u: (((x.pages || [])[0] || {}).content_urls || {}).mobile ? x.pages[0].content_urls.mobile.page : '' }));
    try { localStorage.setItem(key, JSON.stringify({ t: Date.now(), e })); } catch {}
    return e;
  } catch { return []; }
}
const revTodayBox = e => e && e.length ? `<section class="rv-today"><p class="rv-lab">Hoy en la música · ${revDate(Date.now(), { day: 'numeric', month: 'long' })}</p>${e.map(x => `<div><b>${esc(x.y)}</b><p>${esc(x.t)}${/^https:\/\/es\.m\.wikipedia\.org\//.test(x.u) ? ` <a href="${esc(x.u)}" target="_blank" rel="noopener noreferrer">↗</a>` : ''} <button type="button" class="rv-x" data-act="todayp" data-v="${esc(x.y)}">pósters de ${esc(x.y)}</button></p></div>`).join('')}<p class="rv-m">Fuente: Wikipedia (CC BY-SA)</p></section>` : '';
/* Agenda de conciertos y festivales, con mapa */
async function revAgenda(mode) {
  rev.sec = 'agenda'; rev.view = 'agenda'; rev.agMode = mode || rev.agMode || 'list'; revBar(); const my = ++rev.req, box = revBody();
  box.innerHTML = `<div class="rv-tabs">${[['list', 'Próximos'], ['map', 'Mapa'], ['past', 'Pasados']].map(([k, n]) => `<button type="button" class="rv-tab${rev.agMode === k ? ' on' : ''}" data-act="agmode" data-v="${k}">${n}</button>`).join('')}</div><div id="rvAg"><p class="hint">Cargando…</p></div>`;
  try {
    const now = encodeURIComponent(new Date(Date.now() - 6 * 3600e3).toISOString());
    const q = rev.agMode === 'past' ? `starts_at=lt.${now}&order=starts_at.desc` : rev.agMode === 'map' ? 'order=starts_at.desc' : `starts_at=gte.${now}&order=starts_at.asc`;
    const [ev, mg] = await Promise.all([revFetch(`/rest/v1/events?select=*,profiles!created_by(name),posters(thumb_url,image_url)&${q}&limit=200`), revFetch('/rest/v1/artist_managers?select=user_id,artists(name,verified)').catch(() => [])]);
    if (my !== rev.req) return;
    const off = new Set(mg.filter(m => m.artists && m.artists.verified).map(m => m.user_id + '|' + revNk(m.artists.name)));
    ev.forEach(e => { e.official = off.has(e.created_by + '|' + revNk(e.artist)); }); rev.events = ev;
    if (rev.agMode === 'map') return revMap(ev);
    let last = '', h = '';
    ev.forEach((e, i) => {
      const d = new Date(e.starts_at), m = d.toLocaleDateString('es', { month: 'long', year: 'numeric' }); if (m !== last) { h += `<h4 class="rv-h">${esc(m)}</h4>`; last = m; }
      const me = revMe(), mine = me && me.uid === e.created_by, th = e.posters && (revUrl(e.posters.thumb_url) || revUrl(e.posters.image_url));
      h += `<div class="rv-ev"><div class="rv-evd"><b>${d.getDate()}</b><span>${esc(d.toLocaleDateString('es', { weekday: 'short' }))}</span><small>${esc(d.toLocaleTimeString('es', { hour: '2-digit', minute: '2-digit' }))}</small></div><div class="rv-evi"><b>${esc(e.title)}</b>${e.official ? ' <span class="rv-ok" title="Publicado por la banda verificada">✓ Oficial</span>' : ''}<small>${esc([e.artist, e.venue, e.city, e.country].filter(Boolean).join(' · '))}</small>
<div class="rv-pcb">${e.url ? `<a class="rv-like" href="${esc(e.url)}" target="_blank" rel="noopener noreferrer">Info ↗</a>` : ''}<button type="button" class="rv-like" data-act="ics" data-i="${i}">+ Calendario</button>${mine || revIsAdmin() ? `<button type="button" class="rv-x" data-act="delev" data-id="${esc(e.id)}">Borrar</button>` : ''}${me && !mine ? `<button type="button" class="rv-x" data-act="report" data-type="event" data-id="${esc(e.id)}">Denunciar</button>` : ''}</div></div>${th ? `<img src="${esc(th)}" alt="" loading="lazy">` : ''}</div>`;
    });
    $('rvAg').innerHTML = h || `<p class="hint">${rev.agMode === 'past' ? 'Aún no hay eventos pasados.' : 'No hay conciertos próximos en la agenda. Añade uno con +.'}</p>`;
  } catch (e) { if (my === rev.req && $('rvAg')) $('rvAg').innerHTML = `<p class="hint">${esc(/actualizar la base/.test(e.message) ? V4_MSG : e.message)}</p>`; }
}
let revLeafletP = null;
function revLeaflet() {
  return revLeafletP || (revLeafletP = new Promise((ok, no) => {
    const c = document.createElement('link'); c.rel = 'stylesheet'; c.href = 'https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.css'; document.head.appendChild(c);
    const s = document.createElement('script'); s.src = 'https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js'; s.onload = () => ok(window.L); s.onerror = () => { revLeafletP = null; no(new Error('No se pudo cargar el mapa.')); }; document.head.appendChild(s);
  }));
}
async function revMap(ev) {
  const el = $('rvAg'); el.innerHTML = '<div id="rvMap" class="rv-map"></div><p class="rv-m">Mapa: © colaboradores de OpenStreetMap</p>';
  try {
    const Lf = await revLeaflet(); if (!$('rvMap')) return;
    const map = Lf.map('rvMap', { zoomControl: true }).setView([15.2, -86.2], 3);
    Lf.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 18, attribution: '© OpenStreetMap' }).addTo(map);
    const pts = [], now = Date.now();
    ev.filter(e => typeof e.lat === 'number' && typeof e.lng === 'number').forEach(e => {
      const up = new Date(e.starts_at) >= now; pts.push([e.lat, e.lng]);
      const div = document.createElement('div'); div.innerHTML = `<b>${esc(e.title)}</b><br>${esc([e.artist, e.city].filter(Boolean).join(' · '))}<br>${esc(revDate(e.starts_at))}${up ? '' : ' (pasado)'}`;
      Lf.circleMarker([e.lat, e.lng], { radius: 9, color: up ? '#f2c230' : '#8d96a3', fillColor: up ? '#f2c230' : '#8d96a3', fillOpacity: .8, weight: 2 }).addTo(map).bindPopup(div);
    });
    if (pts.length) map.fitBounds(pts, { padding: [30, 30], maxZoom: 9 }); else el.insertAdjacentHTML('beforeend', '<p class="hint">Todavía no hay eventos con ubicación.</p>');
    setTimeout(() => map.invalidateSize(), 200);
  } catch (e) { el.innerHTML = `<p class="hint">${esc(e.message)}</p>`; }
}
async function revNewEvent() {
  if (!(await revNeed())) return;
  const v = await uiDialog({ title: 'Nuevo concierto o festival', fields: [{ key: 't', label: 'Nombre del evento' }, { key: 'a', label: 'Banda o artista' }, { key: 'v', label: 'Lugar', placeholder: 'Ej: Teatro Nacional' }, { key: 'c', label: 'Ciudad' }, { key: 'p', label: 'País', value: store.get('rvPais', '') }, { key: 'd', label: 'Fecha (AAAA-MM-DD)' }, { key: 'h', label: 'Hora (HH:MM)', value: '20:00' }, { key: 'u', label: 'Enlace con información (https://…, opcional)' }], ok: 'Publicar' });
  if (!v) return;
  if ((v.p || '').trim()) store.set('rvPais', (v.p || '').trim().slice(0, 60));
  const t = (v.t || '').trim(), dt = new Date(`${(v.d || '').trim()}T${(v.h || '20:00').trim()}`);
  if (t.length < 3) return toast('Escribe el nombre del evento.', true);
  if (!/^\d{4}-\d{2}-\d{2}$/.test((v.d || '').trim()) || isNaN(dt)) return toast('La fecha debe ser AAAA-MM-DD y la hora HH:MM.', true);
  const url = (v.u || '').trim(); if (url && !/^https:\/\/\S+$/.test(url)) return toast('El enlace debe empezar con https://', true);
  let lat = null, lng = null; const place = [v.v, v.c, v.p].map(x => (x || '').trim()).filter(Boolean).join(', ');
  if ((v.c || '').trim()) { try { const g = await (await withTimeout(fetch(`https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${encodeURIComponent([v.c, v.p].filter(Boolean).join(', '))}`), 7000)).json(); if (g[0]) { lat = +g[0].lat; lng = +g[0].lon; } } catch {} }
  await revFetch('/rest/v1/events', { method: 'POST', prefer: 'return=minimal', body: { title: t.slice(0, 120), artist: (v.a || '').trim().slice(0, 80) || null, venue: (v.v || '').trim().slice(0, 100) || null, city: (v.c || '').trim().slice(0, 80) || null, country: (v.p || '').trim().slice(0, 60) || null, starts_at: dt.toISOString(), url: url || null, lat, lng, created_by: revMe().uid } });
  toast(lat === null && place ? 'Evento publicado (no encontré la ubicación para el mapa).' : 'Evento publicado.'); revAgenda('list');
}
function revIcs(e) {
  const f = d => new Date(d).toISOString().replace(/[-:]/g, '').replace(/\.\d{3}/, ''), x = s => String(s || '').replace(/[\\,;]/g, m => '\\' + m).replace(/\n/g, ' ');
  const ics = ['BEGIN:VCALENDAR', 'VERSION:2.0', 'PRODID:-//Radio de ruta//Revista//ES', 'BEGIN:VEVENT', `UID:${e.id}@radio-de-ruta`, `DTSTAMP:${f(Date.now())}`, `DTSTART:${f(e.starts_at)}`, `DTEND:${f(new Date(e.starts_at).getTime() + 3 * 3600e3)}`, `SUMMARY:${x(e.title)}`, `LOCATION:${x([e.venue, e.city, e.country].filter(Boolean).join(', '))}`, e.url ? `URL:${e.url}` : '', 'END:VEVENT', 'END:VCALENDAR'].filter(Boolean).join('\r\n');
  const u = URL.createObjectURL(new Blob([ics], { type: 'text/calendar' })), a = document.createElement('a'); a.href = u; a.download = (e.title || 'evento').replace(/[^\w\- áéíóúñ]+/gi, '').slice(0, 40) + '.ics'; document.body.appendChild(a); a.click(); a.remove(); setTimeout(() => URL.revokeObjectURL(u), 4000);
}

/* --- Paquete 4 · Contenido y Paquete 5 · Herramientas --- */
const REV_PUB = () => rev.v5off ? '' : `&status=eq.published&publish_at=lte.${encodeURIComponent(new Date().toISOString())}`;
const revTags = s => [...new Set(String(s || '').toLowerCase().normalize('NFC').split(/[\s,#]+/).map(t => t.replace(/[^a-z0-9ñáéíóúü]/g, '').slice(0, 24)).filter(Boolean))].slice(0, 5);
const revTagChips = p => (Array.isArray(p.tags) && p.tags.length) ? `<span class="rv-tags">${p.tags.map(t => `<a href="#" class="rv-tag" data-act="tag" data-v="${esc(t)}">#${esc(t)}</a>`).join('')}</span>` : '';
const revState = p => { if (!p || rev.v5off) return ''; if (p.status === 'draft') return '<span class="rv-stt">Borrador</span>'; if (p.publish_at && new Date(p.publish_at) > new Date()) return `<span class="rv-stt">Programado · ${esc(revDate(p.publish_at, { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }))}</span>`; return ''; };
/* Bloques extra dentro del texto: video de YouTube (enlace solo en su línea) y línea de tiempo (líneas «1991: …») */
const revYT = u => { const m = /^https?:\/\/(?:www\.|m\.)?(?:youtube\.com\/(?:watch\?(?:.*&)?v=|shorts\/|embed\/)|youtu\.be\/)([\w-]{11})(?:[?&#]\S*)?$/.exec(String(u).trim()); return m ? m[1] : null; };
function revBlock(b, inl) {
  const y = revYT(b); if (y) return `<button type="button" class="rv-yt" data-act="yt" data-v="${y}" aria-label="Ver video"><img src="https://i.ytimg.com/vi/${y}/hqdefault.jpg" alt="" loading="lazy"><span>▶</span></button>`;
  const ls = b.split('\n');
  if (ls.length >= 2 && ls.every(l => /^\d{4}\s*[:–-]\s+\S/.test(l))) return `<ol class="rv-tl">${ls.map(l => { const m = /^(\d{4})\s*[:–-]\s+(.*)$/.exec(l); return `<li><b>${m[1]}</b><span>${inl(m[2])}</span></li>`; }).join('')}</ol>`;
  return null;
}
const revGallery = g => (Array.isArray(g) && g.length) ? `<div class="rv-gal">${g.map(u => revPic(u)).filter(Boolean).map(u => `<button type="button" class="rv-galb" data-act="lightbox" data-v="${esc(u)}">${revImgTag(u)}</button>`).join('')}</div>` : '';
function revLightbox(u) {
  const d = document.createElement('div'); d.className = 'rv-lb'; d.innerHTML = `<img src="${esc(u)}" alt=""><button type="button" aria-label="Cerrar">✕</button>`;
  d.addEventListener('click', () => d.remove()); document.body.appendChild(d);
}
/* Lecturas: una por sesión y por artículo; el autor no se cuenta */
function revCountView(p) {
  if (rev.v5off || !p || p.status === 'draft') return; const me = revMe(); if (me && me.uid === p.author_id) return;
  const k = 'rr_v_' + p.id; try { if (sessionStorage.getItem(k)) return; sessionStorage.setItem(k, '1'); } catch {}
  revFetch('/rest/v1/rpc/rr_view', { method: 'POST', body: { p: p.id } }).catch(() => {});
}
/* IA al escribir: corrige, sugiere títulos o resume. Nunca escribe por ti. */
async function revAIWrite(kind) {
  const key = getApiKey('geminiKey'); if (!key) return toast('Pon tu clave de Gemini en Ajustes › Sistema y Claves (API).', true, 6000);
  const text = $('rvText').value.trim(), title = $('rvTitle').value.trim();
  if (text.length < 40) return toast('Escribe un poco más primero (la IA no escribe por ti).');
  const P = { fix: `Corrige SOLO ortografía, acentos y puntuación de este texto en español. No cambies palabras, estilo, orden ni el formato (**, *, ##, >, -). Devuelve únicamente el texto corregido.\n\n${text}`,
    titles: `Propón 5 títulos cortos y atractivos para este artículo de música, uno por línea, sin numerar ni comillas.\n\n${title}\n${text.slice(0, 4000)}`,
    sum: `Escribe un subtítulo de máximo 140 caracteres que resuma este artículo de música. Devuelve solo el subtítulo.\n\n${title}\n${text.slice(0, 4000)}` }[kind];
  toast('Tu IA está leyendo…', false, 15000);
  try {
    const model = setting('geminiModel', '') || setting('geminiModelSelect', 'gemini-3.5-flash-lite');
    const r = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`, { method: 'POST', headers: { 'content-type': 'application/json', 'x-goog-api-key': key }, body: JSON.stringify({ contents: [{ parts: [{ text: P }] }] }) });
    const d = await r.json().catch(() => ({})); if (!r.ok) throw new Error((d.error || {}).message || 'Error ' + r.status);
    const out = (((d.candidates || [])[0] || {}).content || {}).parts?.map(x => x.text || '').join('').trim() || ''; if (!out) throw new Error('La IA no respondió.');
    toast('', false, 1);
    if (kind === 'fix') {
      if (out === text) return toast('No encontré errores. ✓');
      if (await uiConfirm('¿Aplicar la corrección?', out.slice(0, 600) + (out.length > 600 ? '…' : ''), 'Aplicar', false)) { $('rvText').value = out; revSaveDraft(); revStChanged(true); toast('Corrección aplicada.'); }
    } else if (kind === 'titles') {
      const t = await uiDialog({ title: 'Títulos sugeridos', options: out.split('\n').map(x => x.replace(/^[\s\d.\-*"«]+|["»]+$/g, '').trim()).filter(Boolean).slice(0, 5).map(x => ({ label: x, value: x })), cancel: 'Ninguno' });
      if (t) { $('rvTitle').value = t.slice(0, 150); revSaveDraft(); revStChanged(true); }
    } else if (await uiConfirm('¿Usar este subtítulo?', out.slice(0, 200), 'Usar', false)) { $('rvSub').value = out.slice(0, 200); revSaveDraft(); revStChanged(true); }
  } catch (e) { toast(e.message, true, 6000); }
}
/* Buscador general */
function revSearchView(noPush) {
  if (!noPush) revPush(); ++rev.req; rev.view = 'search'; revBar();
  revBody().innerHTML = revHead('Volver', 'Buscar en la Revista') + `<input id="rvGQ" class="rv-search" type="search" placeholder="Artículos, #etiquetas, artistas, pósters, personas, conciertos…" value="${esc(rev.gq || '')}" autocomplete="off"><div id="rvGRes"></div>`;
  setTimeout(() => { const i = $('rvGQ'); if (i) i.focus(); }, 50); if (rev.gq) revSearch();
}
async function revSearch() {
  const raw = ($('rvGQ') || {}).value || ''; rev.gq = raw; const el = $('rvGRes'), my = ++rev.req; if (!el) return;
  const tag = /^#/.test(raw.trim()) ? revTags(raw)[0] : '', t = revPat(raw.replace(/^#/, ''));
  if (!t) { el.innerHTML = '<p class="hint">Escribe algo para buscar. Usa # para buscar por etiqueta.</p>'; return; }
  el.innerHTML = '<p class="hint">Buscando…</p>'; const w = encodeURIComponent('*' + t + '*');
  const safe = p => p.catch(() => []);
  const [ps, as, po, us, ev] = await Promise.all([
    safe(revFetch(`/rest/v1/posts?select=${REV_CARD()}${REV_PUB()}&${tag && !rev.v5off ? `tags=cs.${encodeURIComponent('{' + tag + '}')}` : `or=${encodeURIComponent(`(title.ilike.*${t}*,subtitle.ilike.*${t}*)`)}`}&order=created_at.desc&limit=12`)),
    tag ? [] : safe(revFetch(`/rest/v1/artists?select=id,name,genre,years&name=ilike.${w}&limit=8`)),
    tag ? [] : safe(revFetch(`/rest/v1/posters?select=id,title,artist,year,thumb_url,image_url,kind&or=${encodeURIComponent(`(title.ilike.*${t}*,artist.ilike.*${t}*)`)}&order=created_at.desc&limit=8`)),
    tag ? [] : safe(revFetch(`/rest/v1/profiles?select=id,name,avatar&name=ilike.${w}&limit=8`)),
    tag ? [] : safe(revFetch(`/rest/v1/events?select=id,title,artist,city,starts_at&or=${encodeURIComponent(`(title.ilike.*${t}*,artist.ilike.*${t}*)`)}&order=starts_at.desc&limit=6`))]);
  if (my !== rev.req || !$('rvGRes')) return;
  const sec = (n, h) => h ? `<h4 class="rv-h">${n}</h4>${h}` : '';
  const h = sec('Artículos', ps.length ? revGrid(ps) : '') + sec('Artistas', as.map(a => `<div class="rv-urow rv-click" data-act="artist" data-id="${esc(a.id)}" tabindex="0"><div><b>${esc(a.name)}</b><small>${esc([a.genre, a.years].filter(Boolean).join(' · '))}</small></div></div>`).join('')) +
    sec('Pósters', po.length ? `<div class="rv-prow">${po.map(p => `<figure class="rv-pmini" data-act="poster" data-id="${esc(p.id)}" tabindex="0"><img src="${esc(revUrl(p.thumb_url) || revUrl(p.image_url))}" alt="" loading="lazy"><figcaption>${esc(p.title)}</figcaption></figure>`).join('')}</div>` : '') +
    sec('Personas', us.map(u => `<div class="rv-urow rv-click" data-act="profile" data-id="${esc(u.id)}" tabindex="0">${revAv(u, 36)}<div><b>${esc(u.name)}</b></div></div>`).join('')) +
    sec('Conciertos', ev.map(e => `<div class="rv-urow rv-click" data-act="sec" data-v="agenda" tabindex="0"><div><b>${esc(e.title)}</b><small>${esc([e.artist, e.city, revDate(e.starts_at)].filter(Boolean).join(' · '))}</small></div></div>`).join(''));
  $('rvGRes').innerHTML = h || `<p class="hint">No encontré nada con «${esc(raw)}».</p>`;
}
/* Ediciones mensuales */
async function revEditions() {
  rev.sec = 'art'; rev.view = 'feed'; rev.tab = 'editions'; revBar(); const my = ++rev.req, box = revBody();
  box.innerHTML = '<p class="hint">Cargando…</p>';
  try {
    const a = await revFetch('/rest/v1/editions?select=id,title,intro,issue_month,cover,style,published&order=issue_month.desc&limit=24');
    if (my !== rev.req) return; rev.editions = a;
    box.innerHTML = (revIsAdmin() ? '<p class="hint">Solo tú ves las ediciones sin publicar. Crea una con +.</p>' : '<p class="hint">Cada mes, lo mejor de la Revista en una edición especial.</p>') + (a.length ? `<div class="rv-grid">${a.map((e, i) => { const st = revSt(e.style, { cv: 'm', th: 'clasica', ft: 'editorial', ac: 'rojo' }); return `<article class="rv-card rv-st mag${i === 0 ? ' hero' : ''}" ${revAttrs(st)} data-act="edition" data-id="${esc(e.id)}" tabindex="0">${revMag(e.title, revDate(e.issue_month + 'T12:00', { month: 'long', year: 'numeric' }), e.cover, st, e.issue_month + 'T12:00', '', true)}<div class="rv-in">${e.published ? '' : '<span class="rv-stt">Sin publicar</span>'}<p class="rv-m">${esc((e.intro || '').slice(0, 90))}</p></div></article>`; }).join('')}</div>` : '<p class="hint">Todavía no hay ediciones.</p>');
  } catch (e) { if (my === rev.req) box.innerHTML = `<p class="hint">${esc(/actualizar la base/.test(e.message) ? 'Falta actualizar la base de datos: ejecuta revista_supabase_v5.sql en Supabase (SQL Editor).' : e.message)}</p>`; }
}
async function revEdition(id, noPush) {
  if (!UUID.test(id)) return; if (!noPush) revPush(); rev.view = 'edition'; revBar(); const my = ++rev.req, box = revBody(); box.innerHTML = '<p class="hint">Cargando…</p>';
  try {
    const e = (await revFetch(`/rest/v1/editions?select=*&id=eq.${id}`))[0]; if (!e) { box.innerHTML = revHead('Volver', '') + '<p class="hint">Esa edición no existe.</p>'; return; }
    const items = Array.isArray(e.items) ? e.items : [], pid = items.filter(x => x.t === 'post' && UUID.test(x.id)).map(x => x.id), poid = items.filter(x => x.t === 'poster' && UUID.test(x.id)).map(x => x.id);
    const [ps, po] = await Promise.all([pid.length ? revFetch(`/rest/v1/posts?select=${REV_CARD()}&id=in.(${pid.join(',')})`) : [], poid.length ? revFetch(`/rest/v1/posters?select=${REV_PSEL()}&id=in.(${poid.join(',')})`) : []]);
    if (my !== rev.req) return; rev.edition = e;
    const ord = (arr, ids) => ids.map(i => arr.find(x => x.id === i)).filter(Boolean);
    const st = revSt(e.style, { cv: 'm', th: 'clasica', ft: 'editorial', ac: 'rojo' });
    box.innerHTML = revHead('Ediciones', '', `<span class="rv-sp"></span>${revIsAdmin() ? '<button type="button" class="alt" data-act="editedition">Editar</button>' : ''}<button type="button" class="alt" data-act="shareed">${ICO('share')}</button>`) +
      `<article class="rv-art rv-st" ${revAttrs(st)}>${revHeader(e.title, revDate(e.issue_month + 'T12:00', { month: 'long', year: 'numeric' }), e.cover, st, '', e.issue_month + 'T12:00')}${e.intro ? `<div class="rv-txt" data-dc="${st.dc}">${revFmt(e.intro)}</div>` : ''}</article>` +
      (ps.length ? `<h4 class="rv-h">En esta edición</h4>${revGrid(ord(ps, pid))}` : '') + (po.length ? `<h4 class="rv-h">Pósters de la edición</h4><div class="rv-pgrid">${ord(po, poid).map(p => `<figure class="rv-pcard" data-act="poster" data-id="${esc(p.id)}" tabindex="0"><img src="${esc(revUrl(p.thumb_url) || revUrl(p.image_url))}" alt="" loading="lazy"><figcaption>${revBadge(p)}<b>${esc(p.title)}</b><small>${esc([p.artist, p.year].filter(Boolean).join(' · '))}</small></figcaption></figure>`).join('')}</div>` : '');
    box.scrollIntoView({ block: 'start' });
  } catch (err) { if (my === rev.req) box.innerHTML = revHead('Volver', '') + `<p class="hint">${esc(err.message)}</p>`; }
}
async function revEditEdition(e) {
  if (!revIsAdmin()) return; revPush(); ++rev.req; rev.view = 'editedition'; rev.editE = e ? e.id : null; revBar();
  rev.est = revSt(e && e.style, { cv: 'm', th: 'clasica', ft: 'editorial', ac: 'rojo' }); rev.esel = new Set((e && Array.isArray(e.items) ? e.items : []).map(x => x.t + ':' + x.id));
  const m = e ? e.issue_month.slice(0, 7) : revToday10().slice(0, 7);
  revBody().innerHTML = revHead('Cancelar', e ? 'Editar edición' : 'Nueva edición') + `<div class="rv-form"><label for="rvETitle">Título</label><input id="rvETitle" maxlength="120" value="${esc(e ? e.title : 'Edición ' + revDate(m + '-15T12:00', { month: 'long', year: 'numeric' }))}">
<label for="rvEMonth">Mes (AAAA-MM)</label><input id="rvEMonth" maxlength="7" value="${esc(m)}"><label for="rvEIntro">Introducción</label><textarea id="rvEIntro" rows="4" maxlength="2000">${esc(e ? e.intro || '' : '')}</textarea>
<label>Estética de la portada</label><div id="rvStyle"></div><div id="rvPrev" class="rv-art rv-st"></div><h4 class="rv-h">Elige artículos y pósters</h4><div id="rvEPick"><p class="hint">Cargando…</p></div>
<div class="row" style="margin-top:14px"><button type="button" class="alt" data-act="saveed" data-v="0">Guardar sin publicar</button><button type="button" data-act="saveed" data-v="1">Publicar edición</button>${e ? '<button type="button" class="alt" data-act="deled">Eliminar</button>' : ''}</div></div>`;
  revStChanged();
  const [ps, po] = await Promise.all([revFetch(`/rest/v1/posts?select=id,title,cover,created_at${REV_PUB()}&order=created_at.desc&limit=40`).catch(() => []), revFetch('/rest/v1/posters?select=id,title,artist,thumb_url,image_url&order=created_at.desc&limit=30').catch(() => [])]);
  rev.epick = { ps, po }; if (!$('rvEPick')) return;
  $('rvEPick').innerHTML = [...ps.map(p => ['post', p.id, p.title, revDate(p.created_at)]), ...po.map(p => ['poster', p.id, p.title, 'Póster' + (p.artist ? ' · ' + p.artist : '')])].map(([t, id, n, s]) => `<label class="chk rv-pick"><input type="checkbox" data-pick="${t}:${esc(id)}"${rev.esel.has(t + ':' + id) ? ' checked' : ''}> <span><b>${esc(n)}</b><small>${esc(s)}</small></span></label>`).join('') || '<p class="hint">No hay contenido publicado todavía.</p>';
}
async function revSaveEdition(pub) {
  const title = $('rvETitle').value.trim(), m = $('rvEMonth').value.trim();
  if (title.length < 3) return toast('Escribe un título.', true); if (!/^\d{4}-\d{2}$/.test(m)) return toast('El mes debe ser AAAA-MM.', true);
  const items = [...document.querySelectorAll('#rvEPick [data-pick]:checked')].map(x => { const [t, id] = x.dataset.pick.split(':'); return { t, id }; }).slice(0, 40);
  const first = items.find(x => x.t === 'post'), fp = first && rev.epick.ps.find(p => p.id === first.id), fpo = rev.epick.po.find(p => items.some(x => x.t === 'poster' && x.id === p.id));
  const cover = (fp && revUrl(fp.cover)) || (fpo && revUrl(fpo.image_url)) || null;
  const row = { title: title.slice(0, 120), intro: $('rvEIntro').value.trim().slice(0, 2000) || null, issue_month: m + '-01', cover, style: revStDiff(rev.est, { cv: 'm', th: 'clasica', ft: 'editorial', ac: 'rojo' }), items, published: pub };
  try {
    let id = rev.editE;
    if (id) await revFetch(`/rest/v1/editions?id=eq.${id}`, { method: 'PATCH', body: row, prefer: 'return=minimal' });
    else { const r = await revFetch('/rest/v1/editions', { method: 'POST', body: { ...row, created_by: revMe().uid }, prefer: 'return=representation' }); id = r[0].id; }
    toast(pub ? 'Edición publicada.' : 'Edición guardada.'); if (rev.editE) rev.stack.pop(); rev.view = 'feed'; revEdition(id, true);
  } catch (e) { toast(e.message, true, 6000); }
}
/* Colecciones de pósters */
async function revSaveToCollection() {
  const p = rev.poster; if (!p || !(await revNeed())) return; const me = revMe();
  const cs = await revFetch(`/rest/v1/collections?select=id,title&owner=eq.${me.uid}&order=created_at.desc`);
  let id = await uiDialog({ title: 'Guardar en colección', options: [...cs.map(c => ({ label: c.title, value: c.id })), { label: '+ Nueva colección', value: 'new' }], cancel: 'Cancelar' });
  if (!id) return;
  if (id === 'new') {
    const v = await uiDialog({ title: 'Nueva colección', fields: [{ key: 't', label: 'Nombre', placeholder: 'Mis pósters punk favoritos' }], ok: 'Crear' }); if (!v || (v.t || '').trim().length < 2) return;
    id = (await revFetch('/rest/v1/collections', { method: 'POST', prefer: 'return=representation', body: { owner: me.uid, title: v.t.trim().slice(0, 80) } }))[0].id;
  }
  try { await revFetch('/rest/v1/collection_items', { method: 'POST', prefer: 'return=minimal', body: { collection_id: id, poster_id: p.id } }); toast('Guardado en tu colección.'); }
  catch (e) { toast(/ya existe/i.test(e.message) ? 'Ya estaba en esa colección.' : e.message, true); }
}
async function revCollection(id, noPush) {
  if (!UUID.test(id)) return; if (!noPush) revPush(); rev.view = 'collection'; revBar(); const my = ++rev.req, box = revBody(); box.innerHTML = '<p class="hint">Cargando…</p>';
  try {
    const [c, it] = await Promise.all([revFetch(`/rest/v1/collections?select=*,profiles!owner(name)&id=eq.${id}`), revFetch(`/rest/v1/collection_items?select=poster_id,posters(${REV_PSEL()})&collection_id=eq.${id}&order=created_at.desc`)]);
    if (my !== rev.req) return; const col = c[0]; if (!col) { box.innerHTML = revHead('Volver', '') + '<p class="hint">Esa colección no existe o es privada.</p>'; return; }
    rev.col = col; const me = revMe(), mine = me && me.uid === col.owner, ps = it.map(x => x.posters).filter(Boolean);
    box.innerHTML = revHead('Volver', col.title, `${mine ? `<button type="button" class="alt" data-act="colpriv">${col.public ? 'Hacer privada' : 'Hacer pública'}</button><button type="button" class="alt" data-act="coldel">Eliminar</button>` : ''}`) + `<p class="rv-m">Colección de ${esc(revName(col))} · ${ps.length} pósters${col.public ? '' : ' · privada'}</p>` +
      (ps.length ? `<div class="rv-pgrid">${ps.map(p => `<figure class="rv-pcard"><img src="${esc(revUrl(p.thumb_url) || revUrl(p.image_url))}" alt="" loading="lazy" data-act="poster" data-id="${esc(p.id)}"><figcaption>${revBadge(p)}<b>${esc(p.title)}</b><small>${esc([p.artist, p.year].filter(Boolean).join(' · '))}</small>${mine ? `<button type="button" class="rv-x" data-act="coldelitem" data-id="${esc(p.id)}">Quitar</button>` : ''}</figcaption></figure>`).join('')}</div>` : '<p class="hint">Vacía. Guarda pósters desde la galería con «Guardar en colección».</p>');
  } catch (e) { if (my === rev.req) box.innerHTML = revHead('Volver', '') + `<p class="hint">${esc(e.message)}</p>`; }
}
/* Imágenes para compartir: fondo de pantalla y tarjeta para historias */
async function revImgEl(u) { const r = await fetch(u); if (!r.ok) throw new Error('No pude leer la imagen.'); const b = await r.blob(); const bm = await createImageBitmap(b); return bm; }
function revCanvas(w, h) { const c = document.createElement('canvas'); c.width = w; c.height = h; return [c, c.getContext('2d')]; }
function revCoverFit(g, im, x, y, w, h) { const k = Math.max(w / im.width, h / im.height), iw = im.width * k, ih = im.height * k; g.drawImage(im, x + (w - iw) / 2, y + (h - ih) / 2, iw, ih); }
async function revSaveCanvas(c, name, share) {
  const b = await new Promise(ok => c.toBlob(ok, 'image/jpeg', 0.9)), f = new File([b], name + '.jpg', { type: 'image/jpeg' });
  try { if (share && navigator.canShare && navigator.canShare({ files: [f] })) { await navigator.share({ files: [f] }); return; } } catch (e) { if (e && e.name === 'AbortError') return; }
  const u = URL.createObjectURL(b), a = document.createElement('a'); a.href = u; a.download = f.name; document.body.appendChild(a); a.click(); a.remove(); setTimeout(() => URL.revokeObjectURL(u), 5000); toast('Imagen descargada.');
}
async function revWallpaper() {
  const p = rev.poster; if (!p) return;
  try {
    toast('Preparando fondo…'); const im = await revImgEl(revUrl(p.image_url)), dpr = Math.min(3, window.devicePixelRatio || 1);
    const W = Math.round(Math.min(screen.width, screen.height) * dpr), H = Math.round(Math.max(screen.width, screen.height) * dpr), [c, g] = revCanvas(W, H);
    g.filter = 'blur(40px) brightness(.55)'; revCoverFit(g, im, -60, -60, W + 120, H + 120); g.filter = 'none';
    const k = Math.min(W * 0.92 / im.width, H * 0.8 / im.height), iw = im.width * k, ih = im.height * k; g.drawImage(im, (W - iw) / 2, (H - ih) / 2, iw, ih);
    await revSaveCanvas(c, 'fondo-' + (p.title || 'poster').replace(/[^\w-]+/g, '-').slice(0, 30), false);
  } catch (e) { toast(e.message || 'No pude crear el fondo.', true); }
}
async function revStoryCard(kind) {
  try {
    const p = kind === 'post' ? rev.post : rev.poster; if (!p) return;
    const st = revSt(p.style), [c, g] = revCanvas(1080, 1920), base = REV_THC[st.th] || '#1b1e23', ac = REV_AC[st.ac] || '#f2c230', light = REV_LIGHT.has(st.th);
    g.fillStyle = /^#/.test(base) ? base : (light ? '#f6f3ee' : '#141519'); g.fillRect(0, 0, 1080, 1920);
    const src = kind === 'post' ? revPic(p.cover) : revUrl(p.image_url);
    if (src) { try { const im = await revImgEl(src); if (kind === 'post') revCoverFit(g, im, 0, 0, 1080, 1100); else { const k = Math.min(960 / im.width, 1200 / im.height); g.drawImage(im, (1080 - im.width * k) / 2, 160, im.width * k, im.height * k); } } catch {} }
    if (kind === 'post') { const gr = g.createLinearGradient(0, 700, 0, 1100); gr.addColorStop(0, 'rgba(0,0,0,0)'); gr.addColorStop(1, g.fillStyle); g.fillStyle = gr; g.fillRect(0, 700, 1080, 400); }
    const fam = getComputedStyle(document.querySelector('#revBody .rv-st') || document.body).getPropertyValue('--rfh').trim() || 'sans-serif';
    try { await document.fonts.load(`700 80px ${fam}`); } catch {}
    const text = kind === 'post' ? p.title : p.title, sub = kind === 'post' ? (p.subtitle || 'por ' + revName(p)) : [p.artist, p.year].filter(Boolean).join(' · ');
    g.fillStyle = light ? '#111' : '#fff'; g.font = `700 92px ${fam}`; g.textBaseline = 'top';
    const lines = []; let line = ''; String(text).split(/\s+/).forEach(w => { const t = line ? line + ' ' + w : w; if (g.measureText(t).width > 940 && line) { lines.push(line); line = w; } else line = t; }); if (line) lines.push(line);
    let y = kind === 'post' ? 1140 : 1420; lines.slice(0, 4).forEach(l => { g.fillText(l, 70, y); y += 102; });
    g.font = '500 44px sans-serif'; g.fillStyle = light ? '#444' : '#ccc'; g.fillText(String(sub).slice(0, 60), 70, y + 16);
    g.fillStyle = ac; g.fillRect(70, 1770, 120, 10); g.font = '700 40px sans-serif'; g.fillStyle = light ? '#111' : '#fff'; g.fillText('RADIO DE RUTA · REVISTA', 70, 1800);
    await revSaveCanvas(c, 'radio-de-ruta-' + (kind === 'post' ? 'articulo' : 'poster'), true);
  } catch (e) { toast('No pude crear la imagen.', true); }
}
/* Comparador: original histórico al lado de versiones de la comunidad */
async function revCompare() {
  const p = rev.poster; if (!p || !p.artist) return; revPush(); rev.view = 'compare'; revBar(); const my = ++rev.req;
  const cm = await revFetch(`/rest/v1/posters?select=${REV_PSEL()}&kind=eq.comunidad&artist=ilike.${encodeURIComponent('*' + revPat(p.artist) + '*')}&limit=20`).catch(() => []);
  if (my !== rev.req) return; rev.cmp = cm;
  const pick = i => { const q = cm[i]; $('rvCmpB').innerHTML = q ? `<img src="${esc(revUrl(q.image_url))}" alt=""><p><span class="rv-badge cm">Comunidad</span> ${esc(q.title)} · arte de ${esc(q.designer || revName(q))}</p>` : '<p class="hint">Aún no hay versiones de la comunidad de este artista.</p>'; };
  revBody().innerHTML = revHead('Volver', 'Comparar') + `<div class="rv-cmp"><div><img src="${esc(revUrl(p.image_url))}" alt=""><p><span class="rv-badge og">Original</span> ${esc(p.title)}${p.year ? ' · ' + p.year : ''}</p></div><div id="rvCmpB"></div></div>${cm.length > 1 ? `<div class="rv-tabs">${cm.map((q, i) => `<button type="button" class="rv-tab${i ? '' : ' on'}" data-act="cmppick" data-i="${i}">${esc(q.title.slice(0, 18))}</button>`).join('')}</div>` : ''}`;
  rev.cmpPick = pick; pick(0);
}
/* Internet Archive: solo elementos con licencia libre o de dominio público (enlace, no se copia) */
async function revArchive(q, my) {
  const el = $('rvARes'); if (!el) return; const head = '<h4 class="rv-h">Internet Archive · licencia libre</h4>';
  try {
    const u = 'https://archive.org/advancedsearch.php?q=' + encodeURIComponent(`(${q}) AND (poster OR cartel) AND mediatype:image`) + '&fl[]=identifier&fl[]=title&fl[]=date&fl[]=creator&fl[]=licenseurl&rows=40&output=json';
    const r = await withTimeout(fetch(u), 9000); if (!r.ok) throw 0; const d = await r.json(); if (my !== rev.req || !$('rvARes')) return;
    const ok = ((d.response || {}).docs || []).filter(x => /creativecommons\.org|publicdomain/i.test(x.licenseurl || '')).slice(0, 8);
    el.innerHTML = head + (ok.length ? `<div class="rv-web">${ok.map(x => { const id = String(x.identifier || '').replace(/[^\w.-]/g, ''), y = (/\b(1[89]\d\d|20\d\d)\b/.exec(String(x.date || '')) || [])[1]; const cr = Array.isArray(x.creator) ? x.creator[0] : x.creator; return `<a class="rv-wcard" href="https://archive.org/details/${esc(id)}" target="_blank" rel="noopener noreferrer"><span class="rv-wyear">${esc(y || '—')}</span><b>${esc(String(x.title || id).slice(0, 140))}</b><small>${cr ? 'Autor: ' + esc(String(cr).slice(0, 80)) + ' · ' : ''}${/publicdomain/i.test(x.licenseurl) ? 'Dominio público' : 'Creative Commons'}</small><small class="rv-src">Fuente: archive.org ↗</small></a>`; }).join('')}</div>` : '<p class="hint">Sin resultados con licencia libre.</p>');
  } catch { if ($('rvARes')) $('rvARes').innerHTML = head + '<p class="hint">Internet Archive no respondió ahora.</p>'; }
}
/* Copia de seguridad (admin): descarga todo lo público de la revista en un archivo */
async function revBackup() {
  if (!revIsAdmin()) return; const T = ['profiles', 'posts', 'comments', 'likes', 'follows', 'recommends', 'artists', 'artist_managers', 'posters', 'polls', 'challenges', 'events', 'editions', 'collections', 'collection_items', 'reports', 'rr_settings'];
  const out = { app: 'Radio de ruta · Revista', fecha: new Date().toISOString(), nota: 'Los votos privados y las notificaciones no se incluyen. Las imágenes están en el almacén de Supabase (enlaces).', tablas: {} };
  try {
    for (const t of T) {
      toast(`Copiando ${t}…`, false, 20000); const rows = [];
      for (let off = 0; off < 200000; off += 1000) { const a = await revFetch(`/rest/v1/${t}?select=*&limit=1000&offset=${off}`).catch(() => null); if (!a) break; rows.push(...a); if (a.length < 1000) break; }
      out.tablas[t] = rows;
    }
    const b = new Blob([JSON.stringify(out)], { type: 'application/json' }), u = URL.createObjectURL(b), a = document.createElement('a');
    a.href = u; a.download = `revista-respaldo-${revToday10()}.json`; document.body.appendChild(a); a.click(); a.remove(); setTimeout(() => URL.revokeObjectURL(u), 5000);
    toast('Copia de seguridad descargada.');
  } catch (e) { toast(e.message, true); }
}

const revDrawGal = () => { const el = $('rvGalPrev'); if (el) el.innerHTML = (rev.gal || []).map((u, i) => `<span class="rv-galb">${u.startsWith('data:') ? `<img src="${u}" alt="">` : revImgTag(u)}<button type="button" class="rv-x" data-act="delgal" data-i="${i}">quitar</button></span>`).join(''); };

/* Contraseña olvidada: Supabase manda un enlace al correo; al abrirlo, la app pide la nueva contraseña */
async function revForgot() {
  const v = await uiDialog({ title: 'Recuperar contraseña', text: 'Te enviaremos un enlace a tu correo. Ábrelo en este mismo teléfono.', fields: [{ key: 'email', label: 'Correo de tu cuenta', type: 'email' }], ok: 'Enviar enlace' });
  if (!v) return; const email = (v.email || '').trim();
  if (!/^\S+@\S+\.\S+$/.test(email)) return toast('Revisa el correo.', true);
  const c = revCfg();
  try {
    try { localStorage.setItem('rr_last_auth', 'recover'); } catch {}
    const r = await fetch(`${c.url}/auth/v1/recover?redirect_to=${encodeURIComponent(location.href.split('#')[0])}`, { method: 'POST', headers: { apikey: c.key, 'Content-Type': 'application/json' }, body: JSON.stringify({ email }) });
    if (r.status === 429) return toast('Ya pediste varios enlaces. Espera unos minutos.', true, 6000);
    toast('Si ese correo tiene cuenta, te llegó un enlace. Revisa también la carpeta de spam.', false, 8000);
  } catch (e) { toast(revErr(e.message), true); }
}
// Correo sin confirmar: ofrece mandar el correo de confirmación otra vez
async function revResend(email) {
  if (!(await uiConfirm('Falta confirmar tu correo', 'Tu cuenta existe, pero todavía no confirmaste el correo. ¿Te mando el correo de confirmación otra vez? Ábrelo en este mismo teléfono.', 'Reenviar correo', false))) return;
  const c = revCfg();
  try {
    try { localStorage.setItem('rr_last_auth', 'signup'); } catch {}
    const r = await fetch(`${c.url}/auth/v1/resend?redirect_to=${encodeURIComponent(location.href.split('#')[0])}`, { method: 'POST', headers: { apikey: c.key, 'Content-Type': 'application/json' }, body: JSON.stringify({ type: 'signup', email }) });
    if (r.status === 429) return toast('Ya pediste varios correos. Espera unos minutos.', true, 6000);
    toast(r.ok ? 'Listo: revisa tu correo (y la carpeta de spam) y toca el enlace.' : 'No se pudo reenviar. Inténtalo en unos minutos.', !r.ok, 8000);
  } catch { toast('Sin conexión con el servidor de la revista.', true); }
}
async function revRecover() {
  const d = window.revRecovery; window.revRecovery = null; const c = revCfg();
  if (d.error) return toast(/expired|invalid/i.test(d.error) ? (d.kind === 'signup' ? 'El enlace de confirmación venció o ya se usó. Toca «Entrar» con tu correo: si falta confirmar, te ofrezco mandarlo otra vez.' : 'El enlace ya venció o se usó. Pide uno nuevo en «Olvidé mi contraseña».') : 'No se pudo usar el enlace: ' + d.error, true, 10000);
  if (d.kind === 'signup') { // vino del correo de confirmación: entra directo
    try {
      const u = await (await fetch(c.url + '/auth/v1/user', { headers: { apikey: c.key, Authorization: 'Bearer ' + d.access } })).json();
      if (!u || !u.id) throw 0;
      revSaveSes({ access: d.access, refresh: d.refresh, exp: Date.now() + (d.exp || 3600) * 1000, uid: u.id, name: ((u.user_metadata || {}).name || '').slice(0, 40) });
      toast('¡Correo confirmado! Ya estás dentro.'); await revSocial(true); revBar(); return revSection('art');
    } catch { return toast('Correo confirmado. Ahora toca «Entrar» con tu correo y contraseña.', false, 8000); }
  }
  try {
    const u = await (await fetch(c.url + '/auth/v1/user', { headers: { apikey: c.key, Authorization: 'Bearer ' + d.access } })).json();
    if (!u || !u.id) throw new Error('El enlace ya no es válido. Pide uno nuevo.');
    revSaveSes({ access: d.access, refresh: d.refresh, exp: Date.now() + (d.exp || 3600) * 1000, uid: u.id, name: ((u.user_metadata || {}).name || '').slice(0, 40) });
    for (;;) {
      const v = await uiDialog({ title: 'Nueva contraseña', fields: [{ key: 'p', label: 'Nueva contraseña (mínimo 6)', type: 'password' }, { key: 'q', label: 'Repítela', type: 'password' }], ok: 'Guardar' });
      if (!v) { toast('Puedes cambiarla después repitiendo «Olvidé mi contraseña».'); break; }
      if ((v.p || '').length < 6) { toast('Mínimo 6 caracteres.', true); continue; }
      if (v.p !== v.q) { toast('No coinciden.', true); continue; }
      const r = await fetch(c.url + '/auth/v1/user', { method: 'PUT', headers: { apikey: c.key, Authorization: 'Bearer ' + d.access, 'Content-Type': 'application/json' }, body: JSON.stringify({ password: v.p }) });
      const j = await r.json().catch(() => ({}));
      if (!r.ok) { toast(revErr(j.msg || j.message || j.error_description || 'No se pudo guardar.'), true, 6000); continue; }
      toast('¡Contraseña cambiada! Ya estás dentro.'); break;
    }
    await revSocial(true); revBar(); revSection('art');
  } catch (e) { toast(e.message, true, 7000); }
}

/* --- La bóveda del tiempo: conciertos y festivales históricos elegidos por el curador ---
   Cada concierto entra en cartelera un día y se queda 30 días (o lo que diga el curador).
   Los videos se ven con el reproductor oficial de YouTube, Vimeo o Internet Archive: no se guarda ni se descarga nada. */
const VAULT_PROV = { youtube: 'YouTube', vimeo: 'Vimeo', archive: 'Internet Archive' };
const VAULT_ID = { youtube: /^[\w-]{11}$/, vimeo: /^\d{5,12}$/, archive: /^[A-Za-z0-9_.-]{3,100}$/ };
const VAULT_ACTS = new Set(['bvtab', 'bvopen', 'bvplay', 'bvseek', 'bvcycle', 'bvedit', 'bvsave', 'bvcheck', 'bvnext', 'bvfeature', 'bvhide', 'bvdel', 'bvresched', 'bvcom', 'bvdelc', 'bvshare', 'bvlib', 'bvplaylib', 'bvart', 'bvcur', 'bvpickcov', 'bvnocov', 'bvperm', 'bvundo', 'bvlog', 'bvavt', 'bvadmf', 'bvdiff', 'bvnew', 'bvwatch', 'bvstory']);
const V6_MSG = 'Falta activar la Bóveda en la base de datos: ejecuta otra vez revista_supabase_completo.sql en Supabase (SQL Editor). Se puede ejecutar sin miedo: no borra nada.';
const vaultOkId = v => !!(v && VAULT_PROV[v.provider] && VAULT_ID[v.provider].test(v.video_id || ''));
const vaultDay = s => Date.parse(String(s).slice(0, 10) + 'T12:00:00Z');
const vaultAdd = (s, n) => new Date(vaultDay(s) + n * 864e5).toISOString().slice(0, 10);
const vaultDiff = (a, b) => Math.round((vaultDay(b) - vaultDay(a)) / 864e5);
const vaultState = v => { const t = revToday10(); return v.starts_on > t ? 'soon' : v.ends_on <= t ? 'past' : 'now'; };
const vaultLeft = v => vaultDiff(revToday10(), v.ends_on);
const vaultLeftTxt = v => { const n = vaultLeft(v); return n <= 1 ? 'Último día' : `Se va en ${n} días`; };
const vaultNew = v => vaultState(v) === 'now' && vaultDiff(v.starts_on, revToday10()) <= 2;
const vaultD = (s, o = { day: 'numeric', month: 'short' }) => revDate(String(s).slice(0, 10) + 'T12:00:00', o);
const vaultMeta = v => [v.artist, v.festival, v.year, v.place].filter(Boolean).join(' · ');
const vaultThumb = v => { const c = revPic(v.cover); if (c) return revThumb(c); if (!vaultOkId(v)) return ''; if (v.provider === 'youtube') return `https://i.ytimg.com/vi/${v.video_id}/hqdefault.jpg`; if (v.provider === 'archive') return `https://archive.org/services/img/${v.video_id}`; return ''; };
const vaultPage = v => !vaultOkId(v) ? '' : v.provider === 'youtube' ? `https://www.youtube.com/watch?v=${v.video_id}` : v.provider === 'vimeo' ? `https://vimeo.com/${v.video_id}` : `https://archive.org/details/${v.video_id}`;
const vaultEmbed = (v, t) => !vaultOkId(v) ? '' : v.provider === 'youtube' ? `https://www.youtube-nocookie.com/embed/${v.video_id}?autoplay=1&rel=0&enablejsapi=1&origin=${encodeURIComponent(location.origin)}${t ? '&start=' + t : ''}` : v.provider === 'vimeo' ? `https://player.vimeo.com/video/${v.video_id}?autoplay=1&dnt=1${t ? '#t=' + t + 's' : ''}` : `https://archive.org/embed/${v.video_id}`;
const vaultPlain = t => String(t || '').replace(/\*\*|\*/g, '').replace(/^(#{1,2}|>|-) /gm, '').replace(/\s+/g, ' ').trim();
const vaultClock = n => { if (n == null || !isFinite(n)) return ''; const h = Math.floor(n / 3600), m = Math.floor(n % 3600 / 60), s = String(n % 60).padStart(2, '0'); return h ? `${h}:${String(m).padStart(2, '0')}:${s}` : `${m}:${s}`; };
function vaultSetParse(t) {
  return String(t || '').split('\n').map(l => l.trim()).filter(Boolean).slice(0, 80).map(l => {
    const m = /^(\d{1,2}(?::\d{2}){1,2})\s*[-–·.)]?\s*(.+)$/.exec(l);
    if (!m) return { t: null, s: l.slice(0, 120) };
    const t = m[1].split(':').map(Number).reduce((a, b) => a * 60 + b, 0);
    return { t, s: m[2].slice(0, 120) };
  });
}
const vaultSetText = a => (Array.isArray(a) ? a : []).map(x => (x && x.t != null ? vaultClock(x.t) + ' ' : '') + ((x && x.s) || '')).join('\n');
function vaultParse(u) {
  u = String(u || '').trim();
  const y = revYT(u) || (/^https?:\/\/(?:www\.)?youtube\.com\/live\/([\w-]{11})(?:[?&#]\S*)?$/.exec(u) || [])[1]; if (y) return { provider: 'youtube', video_id: y };
  let m = /^https?:\/\/(?:www\.|player\.)?vimeo\.com\/(?:video\/)?(\d{5,12})(?:[/?#]\S*)?$/.exec(u); if (m) return { provider: 'vimeo', video_id: m[1] };
  m = /^https?:\/\/(?:www\.)?archive\.org\/(?:details|embed)\/([A-Za-z0-9_.-]{3,100})(?:[/?#]\S*)?$/.exec(u); if (m) return { provider: 'archive', video_id: m[1] };
  return null;
}
// Pregunta a la plataforma el título y el canal del video, y si su dueño permite insertarlo en otras páginas.
async function vaultInfo(p) {
  const page = vaultPage(p), enc = encodeURIComponent(page);
  const tries = p.provider === 'youtube' ? [`https://www.youtube.com/oembed?format=json&url=${enc}`, `https://noembed.com/embed?url=${enc}`]
    : p.provider === 'vimeo' ? [`https://vimeo.com/api/oembed.json?url=${enc}`] : [`https://archive.org/metadata/${p.video_id}`];
  for (const u of tries) {
    try {
      const r = await withTimeout(fetch(u), 8000);
      if (r.status === 401 || r.status === 403) return { blocked: true };
      if (r.status === 404) return { missing: true };
      if (!r.ok) continue;
      const j = await r.json();
      if (p.provider === 'archive') {
        const m = j && j.metadata; if (!m || !m.identifier) return { missing: true };
        const one = x => String(Array.isArray(x) ? x[0] : x || '').trim();
        return { title: one(m.title), author: one(m.creator) || one(m.uploader) || 'Internet Archive', year: +(one(m.date).slice(0, 4)) || null, license: one(m.licenseurl), desc: one(m.description).replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim() };
      }
      if (j && j.error) { if (/401|unauthori|embed/i.test(j.error)) return { blocked: true }; if (/404|not found/i.test(j.error)) return { missing: true }; continue; }
      if (j && j.title) return { title: String(j.title), author: String(j.author_name || '') };
    } catch {}
  }
  return null; // no se pudo comprobar (sin conexión o la plataforma no respondió)
}
/* Curadores */
async function vaultCurators() {
  let a;
  try { a = await revFetch('/rest/v1/vault_curators?select=user_id,can_create,can_edit,can_delete,created_at,profiles(name,avatar)&order=created_at.asc'); }
  catch (e) { if (/actualizar la base/.test(e.message)) throw e; a = (await revFetch('/rest/v1/vault_curators?select=user_id,profiles(name,avatar)')).map(x => ({ ...x, can_create: true, can_edit: true, can_delete: true })); }
  rev.curators = new Set(a.map(x => x.user_id)); rev.curPerm = new Map(a.map(x => [x.user_id, x])); rev.curList = a; return a;
}
const revIsCurator = () => revIsAdmin() || !!(rev.me && rev.curators && rev.curators.has(rev.me.id));
// Permisos de cada curador: 'create' (programar), 'edit' (editar), 'delete' (borrar). El admin puede todo.
const vaultCan = w => revIsAdmin() || !!(rev.me && rev.curPerm && rev.curPerm.get(rev.me.id) && rev.curPerm.get(rev.me.id)['can_' + w]);
/* El concierto del día: el fijado por el curador; si no, el que entró hoy; si no, va rotando entre los de la cartelera. */
function vaultOfDay(now) {
  if (!now.length) return null; const t = revToday10();
  const f = now.find(v => v.featured_on === t) || now.filter(v => v.starts_on === t).sort((a, b) => (b.created_at || '').localeCompare(a.created_at || ''))[0];
  if (f) return f;
  const s = [...now].sort((a, b) => a.starts_on.localeCompare(b.starts_on) || String(a.id).localeCompare(String(b.id)));
  return s[Math.floor(vaultDay(t) / 864e5) % s.length];
}
const vaultRibbon = v => { const st = vaultState(v); return st === 'soon' ? `<span class="bv-rib">Entra el ${esc(vaultD(v.starts_on))}</span>` : st === 'past' ? `<span class="bv-rib bv-rib0">Estuvo hasta el ${esc(vaultD(vaultAdd(v.ends_on, -1)))}</span>` : vaultNew(v) ? '<span class="bv-rib bv-ribn">Nuevo</span>' : `<span class="bv-rib${vaultLeft(v) <= 3 ? ' bv-ribw' : ''}">${esc(vaultLeftTxt(v))}</span>`; };
const vaultImg = (v, cls = '') => { const t = vaultThumb(v); return t ? `<img${cls ? ` class="${cls}"` : ''} src="${esc(t)}" alt="" loading="lazy" decoding="async"${v.provider === 'youtube' && !revPic(v.cover) ? ` data-bvt="${esc(v.id)}"` : ''}>` : `<span class="bv-noimg">${esc((v.festival || v.title || '?').slice(0, 1))}</span>`; };
const vaultCard = v => `<button type="button" class="bv-card${v.hidden ? ' bv-hid' : ''}" data-act="bvopen" data-id="${esc(v.id)}"><span class="bv-frame">${vaultImg(v)}${vaultRibbon(v)}</span><b>${esc(v.title)}</b><small>${esc(vaultMeta(v))}</small>${v.synopsis ? `<small class="bv-syn">${esc(v.synopsis)}</small>` : ''}${v.hidden ? '<small class="bv-warn">Oculto (solo lo ven los curadores)</small>' : ''}</button>`;
// YouTube devuelve una miniatura gris de 120 px cuando el video ya no existe: así se avisa sin pedir nada más.
function vaultWatchThumbs(root) {
  (root || document).querySelectorAll('img[data-bvt]').forEach(im => {
    const bad = () => { const c = im.closest('.bv-card,.bv-hero,.bv-play'); if (c) c.classList.add('bv-gone'); if (im.closest('.bv-play')) { const n = $('bvGone'); if (n) n.hidden = false; } };
    const chk = () => { if (im.naturalWidth && im.naturalWidth <= 120) bad(); };
    if (im.complete) chk(); else { im.addEventListener('load', chk, { once: true }); im.addEventListener('error', bad, { once: true }); }
  });
}
/* Portada de la sección */
async function revVault(tab) {
  rev.sec = 'vault'; if (rev.view !== 'vault') rev.stack = []; rev.view = 'vault'; rev.vTab = tab || rev.vTab || 'now'; revBar(); const my = ++rev.req, box = revBody();
  const t = (v, n) => `<button type="button" class="rv-tab${rev.vTab === v ? ' on' : ''}" data-act="bvtab" data-v="${v}">${n}</button>`;
  box.innerHTML = `<header class="bv-mast"><p class="bv-kick">Conciertos y festivales históricos · cartelera curada</p><h2>La bóveda del tiempo</h2><p class="bv-sub">Cada concierto se queda 30 días en cartelera y luego vuelve a la bóveda. Elige uno y dale play.</p></header><div class="rv-tabs bv-tabs">${t('now', 'En cartelera')}${t('soon', 'Próximamente')}${t('past', 'Ya pasaron')}</div><div id="bvBox"><p class="hint">Abriendo la bóveda…</p></div>`;
  try {
    const today = revToday10(), sel = 'select=*,profiles!created_by(name)';
    const q = rev.vTab === 'past' ? `ends_on=lte.${today}&order=ends_on.desc&limit=60` : `ends_on=gt.${today}&order=starts_on.asc&limit=200`;
    const [rows] = await Promise.all([revFetch(`/rest/v1/vault?${sel}&${q}`), vaultCurators().catch(() => { rev.curators = rev.curators || new Set(); }), revMe() ? revSocial() : null]);
    if (my !== rev.req) return;
    revBar(); rev.vRows = rows;
    const el = $('bvBox'), cur = revIsCurator();
    if (rev.vTab === 'soon') {
      const s = rows.filter(v => vaultState(v) === 'soon');
      el.innerHTML = s.length ? `<p class="rv-m">Lo que el curador ya programó. Entra en cartelera en la fecha indicada.</p><div class="bv-grid">${s.map(vaultCard).join('')}</div>` : `<p class="hint">Todavía no hay estrenos programados.${vaultCan('create') ? ' Toca + para programar uno.' : ''}</p>`;
    } else if (rev.vTab === 'past') {
      el.innerHTML = rows.length ? `<p class="rv-m">Conciertos que ya salieron de la cartelera. Puedes leer su ficha y ver dónde está publicado el video.</p><div class="bv-grid">${rows.map(vaultCard).join('')}</div>` : '<p class="hint">Aún no ha salido ningún concierto de la cartelera.</p>';
    } else {
      const now = rows.filter(v => vaultState(v) === 'now' && (!v.hidden || cur)), day = vaultOfDay(now.filter(v => !v.hidden));
      const cycles = [...new Set(now.map(v => v.cycle).filter(Boolean))];
      if (rev.vCycle && !cycles.includes(rev.vCycle)) rev.vCycle = '';
      const list = now.filter(v => !rev.vCycle || v.cycle === rev.vCycle);
      const hero = day ? `<button type="button" class="bv-hero" data-act="bvopen" data-id="${esc(day.id)}"><span class="bv-frame">${vaultImg(day)}<span class="bv-shade"></span><span class="bv-hcap"><span class="bv-lab">Concierto del día · ${esc(revDate(Date.now(), { day: 'numeric', month: 'long' }))}</span><b>${esc(day.title)}</b><small>${esc(vaultMeta(day))}</small><span class="bv-go">▶ Ver ahora <i>${esc(vaultLeftTxt(day))}</i></span></span></span>${day.synopsis ? `<span class="bv-quote bv-hsyn">${esc(day.synopsis.slice(0, 220))}${day.synopsis.length > 220 ? '…' : ''}</span>` : day.note ? `<span class="bv-quote">«${esc(vaultPlain(day.note).slice(0, 170))}${vaultPlain(day.note).length > 170 ? '…' : ''}»</span>` : ''}</button>` : '';
      const by = (rev.curList || []).filter(c => c.profiles);
      el.innerHTML = now.length ? `${hero}
${cycles.length ? `<div class="bv-cycles"><span class="rv-m">Ciclos:</span><button type="button" class="rv-tab${rev.vCycle ? '' : ' on'}" data-act="bvcycle" data-v="">Todos</button>${cycles.map(c => `<button type="button" class="rv-tab${rev.vCycle === c ? ' on' : ''}" data-act="bvcycle" data-v="${esc(c)}">${esc(c)}</button>`).join('')}</div>` : ''}
<h4 class="rv-h">En cartelera · ${list.length}</h4><div class="bv-grid">${list.map(vaultCard).join('')}</div>` : `<div class="bv-empty"><b>La bóveda está cerrada por ahora.</b><p>El curador está eligiendo los próximos conciertos.${vaultCan('create') ? ' Toca + para programar el primero.' : ''}</p></div>`;
      el.insertAdjacentHTML('beforeend', by.length ? `<p class="bv-curs">Curado por ${by.map(c => `<a href="#" data-act="profile" data-id="${esc(c.user_id)}">${revAv(c.profiles, 22)} ${esc(c.profiles.name)}</a>`).join(' ')}</p>` : '');
    }
    el.insertAdjacentHTML('beforeend', '<p class="bv-legal">Los videos se ven con el reproductor oficial de YouTube, Vimeo o Internet Archive, tal como los publicaron sus dueños. La Bóveda no guarda ni descarga videos; si alguno se retira de la plataforma, deja de verse aquí.</p>');
    vaultWatchThumbs(el);
  } catch (e) { if (my === rev.req && $('bvBox')) $('bvBox').innerHTML = `<p class="hint">${esc(/actualizar la base/.test(e.message) ? V6_MSG : e.message)}</p>`; }
}
/* Ficha de un concierto */
async function revVaultOpen(id, noPush) {
  if (!UUID.test(id)) return;
  if (!noPush) revPush();
  rev.sec = 'vault'; rev.view = 'vitem'; const my = ++rev.req, box = revBody(); box.innerHTML = '<p class="hint">Cargando…</p>'; revBar();
  try {
    const [v, cm] = await Promise.all([revFetch(`/rest/v1/vault?select=*,profiles!created_by(name,avatar)&id=eq.${id}`), revFetch(`/rest/v1/vault_comments?select=*,profiles!author_id(name,avatar)&vault_id=eq.${id}&order=created_at.asc&limit=300`), vaultCurators().catch(() => {}), revMe() ? revSocial() : null]);
    if (my !== rev.req) return;
    if (!v[0]) { box.innerHTML = revHead('Volver', '') + '<p class="hint">Ese concierto ya no está en la bóveda.</p>'; return; }
    rev.vItem = v[0]; rev.vComs = cm; rev.vArt = null; rev.vWatch = false;
    if (revMe() && !rev.v8off) { try { rev.vWatch = (await revFetch(`/rest/v1/vault_watch?select=vault_id&vault_id=eq.${id}`)).length > 0; } catch (e) { if (/vault_watch|schema cache/i.test(e.message + (e.raw || ''))) rev.v8off = true; } }
    if (my !== rev.req) return;
    revVaultDraw(); vaultLiveLoop(id);
    revFetch(`/rest/v1/artists?select=id,name&name=ilike.${encodeURIComponent(revPat(v[0].artist || '') || '-')}&limit=3`).then(a => { const x = (a || []).find(z => revNk(z.name) === revNk(rev.vItem.artist)); if (x && my === rev.req) { rev.vArt = x; const b = $('bvArt'); if (b) b.hidden = false; } }).catch(() => {});
    if (typeof libAll === 'function' && v[0].artist) libAll().then(all => { const n = revNk(v[0].artist); if (my === rev.req && all.some(r => revNk(r.artist) === n)) { const b = $('bvLib'); if (b) b.hidden = false; } }).catch(() => {});
  } catch (e) { if (my === rev.req) box.innerHTML = revHead('Volver', '') + `<p class="hint">${esc(/actualizar la base/.test(e.message) ? V6_MSG : e.message)}</p>`; }
}
function revVaultDraw() {
  const v = rev.vItem, st = vaultState(v), cur = revIsCurator(), me = revMe(), canPlay = st === 'now' || cur, prov = VAULT_PROV[v.provider] || '';
  const status = st === 'now' ? `<span class="bv-on">● En cartelera</span> ${esc(vaultLeftTxt(v))} · hasta el ${esc(vaultD(vaultAdd(v.ends_on, -1), { day: 'numeric', month: 'long' }))}` : st === 'soon' ? `<span class="bv-on bv-soon">● Próximamente</span> entra el ${esc(vaultD(v.starts_on, { day: 'numeric', month: 'long' }))}` : `Salió de cartelera · estuvo del ${esc(vaultD(v.starts_on))} al ${esc(vaultD(vaultAdd(v.ends_on, -1)))}`;
  const set = (Array.isArray(v.setlist) ? v.setlist : []).filter(x => x && x.s);
  const cm = c => `<div class="rv-c"><p class="rv-m"><a href="#" data-act="profile" data-id="${esc(c.author_id)}">${revAv(c.profiles, 22)} <b>${esc(revName(c))}</b></a> · ${revDate(c.created_at)}${me && (me.uid === c.author_id || cur) ? ` <button type="button" class="rv-x" data-act="bvdelc" data-id="${esc(c.id)}">borrar</button>` : ''}${me && me.uid !== c.author_id ? ` <button type="button" class="rv-x" data-act="report" data-type="vcomment" data-id="${esc(c.id)}">denunciar</button>` : ''}</p><p>${esc(c.body)}</p></div>`;
  const ed = vaultCan('edit');
  const tools = cur ? `<div class="bv-tools"><span class="rv-m">Curador:</span>${ed ? `<button type="button" class="alt" data-act="bvedit">Editar</button>${st === 'now' ? `<button type="button" class="alt" data-act="bvfeature">${v.featured_on === revToday10() ? 'Es el concierto del día ✓' : 'Hacerlo concierto del día'}</button>` : ''}${st === 'past' ? '<button type="button" class="alt" data-act="bvresched">Volver a programar</button>' : ''}<button type="button" class="alt" data-act="bvhide">${v.hidden ? 'Mostrar' : 'Ocultar'}</button>` : ''}${vaultCan('delete') ? '<button type="button" class="alt" data-act="bvdel">Borrar</button>' : ''}<button type="button" class="alt" data-act="bvlog">Historial de cambios</button>${!ed ? '<span class="rv-m">No tienes permiso para editar; pídeselo al admin.</span>' : ''}</div><div id="bvLog"></div>` : '';
  revBody().innerHTML = revHead('Volver', '', '<span class="rv-sp"></span>') + `<article class="bv-item">
${canPlay ? `<div class="bv-play" id="bvPlay"><button type="button" class="bv-poster" data-act="bvplay" aria-label="Ver el concierto">${vaultImg(v)}<span class="bv-pbtn">▶</span></button></div>` : `<div class="bv-play bv-closed">${vaultImg(v)}<span>${st === 'soon' ? 'Se estrena el ' + esc(vaultD(v.starts_on, { day: 'numeric', month: 'long' })) : 'Ya salió de la cartelera'}</span></div>`}
<p class="hint bv-gonemsg" id="bvGone" hidden>Parece que este video ya no está disponible en ${esc(prov)}.${cur ? ' Ocúltalo o cámbialo por otro enlace.' : ''}</p>
<p class="bv-status">${status}${v.hidden ? ' · <b>Oculto</b>' : ''}</p>${vaultLiveHtml(v)}
<h2 class="bv-t">${esc(v.title)}</h2><p class="bv-meta">${esc(vaultMeta(v))}${v.cycle ? ` <button type="button" class="rv-tag" data-act="bvcycle" data-v="${esc(v.cycle)}">Ciclo: ${esc(v.cycle)}</button>` : ''}</p>
${v.synopsis ? `<section class="bv-synb"><p class="bv-lab">Sinopsis</p><p>${esc(v.synopsis)}</p></section>` : cur ? '<p class="bv-warn">Falta la sinopsis de este concierto.</p>' : ''}
<p class="bv-src">Fuente: video ${v.source_name ? `de <b>${esc(v.source_name)}</b> ` : ''}en ${esc(prov)}${v.official ? ' <span class="rv-ok" title="El curador verificó que es el canal oficial">✓ Canal oficial</span>' : ''} · <a href="${esc(vaultPage(v))}" target="_blank" rel="noopener noreferrer">Abrir en ${esc(prov)} ↗</a></p>
<div class="rv-pbtns"><button type="button" class="rv-like" data-act="bvplaylib" id="bvLib" hidden>${ICO('play')} Escuchar a ${esc(v.artist || '')} en mi biblioteca</button><button type="button" class="rv-like" data-act="bvart" id="bvArt" hidden>Perfil en la Revista</button>${me && (st === 'soon' || vaultLive(v) === 'pre') ? `<button type="button" class="rv-like${rev.vWatch ? ' on' : ''}" data-act="bvwatch" aria-pressed="${!!rev.vWatch}">${ICO('bell')} ${rev.vWatch ? 'Te avisaremos' : 'Avísame'}</button>` : ''}<button type="button" class="rv-like" data-act="bvstory">${ICO('image')} Imagen para historias</button><button type="button" class="rv-like" data-act="bvshare">${ICO('share')} Compartir</button>${me && !cur ? `<button type="button" class="rv-x" data-act="report" data-type="vault" data-id="${esc(v.id)}">Denunciar</button>` : ''}</div>
${tools}
${v.note ? `<section class="bv-note"><p class="bv-lab">Nota del curador</p><div class="rv-txt">${revFmt(v.note)}</div>${v.profiles ? `<p class="rv-m">— <a href="#" data-act="profile" data-id="${esc(v.created_by)}">${esc(v.profiles.name)}</a></p>` : ''}</section>` : ''}
${set.length ? `<section><h4 class="rv-h">Lista de canciones</h4><ol class="bv-set">${set.map(x => `<li>${x.t != null && canPlay && v.provider !== 'archive' ? `<button type="button" class="bv-seek" data-act="bvseek" data-v="${x.t}"><i>${vaultClock(x.t)}</i> ${esc(x.s)}</button>` : `${x.t != null ? `<i>${vaultClock(x.t)}</i> ` : ''}${esc(x.s)}`}</li>`).join('')}</ol>${canPlay && v.provider !== 'archive' && set.some(x => x.t != null) ? '<p class="rv-m">Toca una canción para saltar a ese momento del video.</p>' : ''}</section>` : ''}
<section class="rv-com"><h4 class="rv-h" id="bvComH">${vaultLive(v) === 'live' ? '🔴 Chat en vivo' : 'Comentarios'} (${rev.vComs.length})</h4><div id="bvComs">${rev.vComs.map(cm).join('') || '<p class="hint">¿Lo viste? Cuenta qué te pareció.</p>'}</div><textarea id="bvCom" rows="3" maxlength="1000" placeholder="Escribe un comentario…"></textarea><button type="button" data-act="bvcom">Comentar</button></section>
</article>`;
  vaultWatchThumbs(revBody()); rev.vCm = cm;
}
function vaultPlay(t) {
  const v = rev.vItem, box = $('bvPlay'); if (!v || !box) return;
  if (!t && vaultLive(v) === 'live') t = Math.max(0, Math.floor((Date.now() - Date.parse(v.live_at)) / 1000)); // función en vivo: todos ven el mismo momento
  const src = vaultEmbed(v, t); if (!src) return;
  box.innerHTML = `<div class="rv-ytf bv-ytf"><iframe src="${esc(src)}" title="${esc(v.title)}" allow="autoplay; encrypted-media; picture-in-picture; fullscreen" allowfullscreen referrerpolicy="strict-origin-when-cross-origin"></iframe></div>`;
  vaultYtWatch(box, v, t);
  if (t) box.scrollIntoView({ behavior: 'smooth', block: 'center' });
}
/* Si YouTube no deja insertar el video (el dueño lo bloqueó fuera de YouTube), se muestra un aviso claro con «Ver en YouTube». */
function vaultYtWatch(box, v, t) {
  const fr = box.querySelector('iframe'); if (!fr || v.provider !== 'youtube') return;
  const hola = () => { try { fr.contentWindow.postMessage(JSON.stringify({ event: 'listening', id: 1, channel: 'widget' }), '*'); } catch {} };
  fr.addEventListener('load', () => { hola(); setTimeout(hola, 900); });
  const oir = e => {
    if (!fr.isConnected) return window.removeEventListener('message', oir);
    if (e.source !== fr.contentWindow || !/^https:\/\/www\.youtube(-nocookie)?\.com$/.test(e.origin)) return;
    let d; try { d = typeof e.data === 'string' ? JSON.parse(e.data) : e.data; } catch { return; }
    if (!d || d.event !== 'onError' || ![100, 101, 150, 153].includes(+d.info)) return;
    window.removeEventListener('message', oir);
    const borrado = +d.info === 100, url = `https://www.youtube.com/watch?v=${encodeURIComponent(v.video_id)}${t ? '&t=' + t + 's' : ''}`;
    box.innerHTML = `<div class="bv-blocked">${vaultImg(v)}<div><b>${borrado ? 'Este video ya no está disponible en YouTube.' : 'El dueño de este video no permite verlo fuera de YouTube.'}</b><p>${borrado ? 'Es posible que lo hayan borrado o puesto privado.' : 'Ábrelo en YouTube para verlo completo.'}</p>${borrado ? '' : `<a class="rv-like" href="${esc(url)}" target="_blank" rel="noopener noreferrer">▶ Ver en YouTube</a>`}${vaultCan('edit') ? '<p class="rv-m">Curador: busca otra copia oficial que permita verse aquí.</p>' : ''}</div></div>`;
  };
  window.addEventListener('message', oir);
}
/* Editor del curador */
function revVaultEdit(v) {
  if (!vaultCan(v && v.id ? 'edit' : 'create')) return toast(v && v.id ? 'No tienes permiso para editar la Bóveda. Pídeselo al admin.' : 'No tienes permiso para programar en la Bóveda. Pídeselo al admin.', true, 5000);
  revPush(); ++rev.req; rev.view = 'editvault'; rev.vEdit = v ? { ...v } : null; rev.vCov = v ? v.cover || '' : ''; rev.vChk = null; revBar();
  const x = v || {}, rows = rev.vRows || [], cycles = [...new Set(rows.map(r => r.cycle).filter(Boolean))];
  const start = x.starts_on || vaultFree(rows);
  revBody().innerHTML = revHead('Cancelar', v ? 'Editar concierto' : 'Programar un concierto') + `<div class="rv-form bv-form">
<p class="bv-rule"><b>Siempre legal:</b> usa videos publicados por el artista, el festival, el sello, una televisora o un archivo autorizado. La Bóveda solo los inserta con el reproductor oficial; nunca se descargan.</p>
<label for="bvUrl">Enlace del video (YouTube, Vimeo o Internet Archive)</label><div class="bv-row"><input id="bvUrl" inputmode="url" autocomplete="off" placeholder="https://www.youtube.com/watch?v=…" value="${esc(vaultPage(x))}"><button type="button" class="alt" data-act="bvcheck">Comprobar</button></div>
<p class="rv-m" id="bvChk"></p>
<label for="bvT">Título del concierto</label><input id="bvT" maxlength="140" value="${esc(x.title || '')}" placeholder="Ej: Café Tacvba en Vive Latino">
<label for="bvSy">Sinopsis <span class="rv-m">(obligatoria: de qué trata el concierto, en 2 a 5 frases)</span></label><textarea id="bvSy" rows="4" maxlength="1500" placeholder="Ej: En 1997, frente a 80 000 personas en Bogotá, la banda repasa sus clásicos en uno de los shows más recordados del festival…">${esc(x.synopsis || '')}</textarea><p class="rv-m bv-cnt" id="bvSyN"></p>
<label for="bvA">Artista o banda</label><input id="bvA" maxlength="100" value="${esc(x.artist || '')}">
<label for="bvF">Festival o evento</label><input id="bvF" maxlength="100" value="${esc(x.festival || '')}" placeholder="Ej: Rock al Parque">
<div class="bv-row"><div><label for="bvY">Año</label><input id="bvY" inputmode="numeric" maxlength="4" value="${esc(x.year || '')}"></div><div><label for="bvP">Lugar</label><input id="bvP" maxlength="120" value="${esc(x.place || '')}" placeholder="Ciudad, país"></div></div>
<label for="bvS">Canal o fuente que lo publicó</label><input id="bvS" maxlength="100" value="${esc(x.source_name || '')}" placeholder="Se llena al comprobar el enlace">
<label class="chk"><input type="checkbox" id="bvO"${x.official ? ' checked' : ''}> Es el canal oficial (del artista, festival, sello o televisora) y lo verifiqué</label>
<label for="bvN">Nota del curador</label><textarea id="bvN" rows="6" maxlength="3000" placeholder="Por qué elegiste este concierto, qué pasaba en ese momento, qué no hay que perderse…">${esc(x.note || '')}</textarea>
<label for="bvL">Lista de canciones (una por línea; con el minuto se puede saltar a esa parte)</label><textarea id="bvL" rows="6" placeholder="0:00 Intro&#10;3:42 Primera canción&#10;1:02:10 Última canción">${esc(vaultSetText(x.setlist))}</textarea>
<label for="bvC">Ciclo (opcional)</label><input id="bvC" maxlength="60" list="bvCl" value="${esc(x.cycle || '')}" placeholder="Ej: Rock en español en vivo, años 90"><datalist id="bvCl">${cycles.map(c => `<option value="${esc(c)}">`).join('')}</datalist>
<div class="bv-row"><div><label for="bvD">Entra en cartelera</label><input id="bvD" type="date" value="${esc(start)}"></div><div><label for="bvN2">Días en cartelera</label><input id="bvN2" type="number" min="1" max="365" value="${esc(x.days || 30)}"></div></div>
<label for="bvLive">Función en vivo (opcional): día y hora para verlo todos juntos con chat</label><input id="bvLive" type="datetime-local" value="${esc(vaultLocalDT(x.live_at))}">
<p class="rv-m"><button type="button" class="rv-x" data-act="bvnext">Usar el siguiente día libre</button> · Un estreno por día hace que la cartelera rote sola, como en el cine.</p>
<label>Imagen propia (opcional; si no, se usa la del video)</label><div id="bvCov" class="bv-cov"></div><div class="rv-pcb"><button type="button" class="alt" data-act="bvpickcov">Elegir imagen</button><button type="button" class="alt" data-act="bvnocov">Quitar</button></div><input type="file" id="bvCovFile" accept="image/*" hidden>
<div class="row" style="margin-top:16px"><button type="button" data-act="bvsave">${v ? 'Guardar cambios' : 'Programar'}</button></div></div>`;
  vaultDrawCov();
  $('bvCovFile').onchange = async e => { const f = e.target.files && e.target.files[0]; e.target.value = ''; if (!f) return; try { rev.vCov = (await revResize(f, 1280, 0.84)).data(); vaultDrawCov(); } catch { toast('No se pudo leer esa imagen.', true); } };
  $('bvUrl').addEventListener('change', () => { rev.vChk = null; vaultCheck(); });
  const syn = () => { const n = $('bvSy').value.trim().length; $('bvSyN').textContent = `${n} / 1500${n < 40 ? ' · mínimo 40 caracteres' : ''}`; }; $('bvSy').addEventListener('input', syn); syn();
}
const vaultDrawCov = () => { const el = $('bvCov'); if (el) el.innerHTML = revPic(rev.vCov) ? `<img src="${esc(revPic(rev.vCov))}" alt="">` : '<span class="rv-m">Sin imagen propia</span>'; };
function vaultFree(rows) { // siguiente día sin estreno, desde hoy
  const used = new Set((rows || []).filter(r => !rev.vEdit || r.id !== rev.vEdit.id).map(r => r.starts_on)); let d = revToday10();
  for (let i = 0; i < 400 && used.has(d); i++) d = vaultAdd(d, 1);
  return d;
}
async function vaultCheck() {
  const out = $('bvChk'), p = vaultParse($('bvUrl').value); if (!out) return null;
  if (!p) { out.textContent = $('bvUrl').value.trim() ? 'Ese enlace no es de YouTube, Vimeo ni Internet Archive.' : ''; return null; }
  out.textContent = 'Comprobando con ' + VAULT_PROV[p.provider] + '…';
  const i = await vaultInfo(p); if (!$('bvChk')) return null;
  rev.vChk = { ...p, info: i };
  if (i && i.blocked) out.innerHTML = '<b class="bv-bad">✗ Su dueño no permite verlo fuera de ' + esc(VAULT_PROV[p.provider]) + '.</b> Elige otro video.';
  else if (i && i.missing) out.innerHTML = '<b class="bv-bad">✗ Ese video no existe o es privado.</b>';
  else if (i) {
    out.innerHTML = `<b class="bv-good">✓ Se puede insertar.</b> «${esc(i.title)}»${i.author ? ` · publicado por <b>${esc(i.author)}</b>` : ''}${i.license ? ` · licencia: ${esc(i.license)}` : ''}. Revisa que sea una fuente oficial.`;
    if (!$('bvT').value.trim()) $('bvT').value = i.title.slice(0, 140);
    if (!$('bvS').value.trim() && i.author) $('bvS').value = i.author.slice(0, 100);
    if (!$('bvY').value.trim() && i.year) $('bvY').value = i.year;
    if (!$('bvSy').value.trim() && i.desc) { $('bvSy').value = i.desc.slice(0, 1500); $('bvSy').dispatchEvent(new Event('input')); }
  } else out.textContent = `No pude comprobarlo ahora con ${VAULT_PROV[p.provider]}. Puedes guardarlo igual; revisa tú que el video sea de una fuente oficial y que se pueda insertar.`;
  return rev.vChk;
}
async function revVaultSave() {
  const val = id => ($(id).value || '').trim(), p = vaultParse(val('bvUrl'));
  if (!p) return toast('Pega el enlace del video (YouTube, Vimeo o Internet Archive).', true);
  let chk = rev.vChk && rev.vChk.video_id === p.video_id && rev.vChk.provider === p.provider ? rev.vChk : await vaultCheck();
  if (chk && chk.info && (chk.info.blocked || chk.info.missing)) return toast(chk.info.blocked ? 'Ese video no se puede insertar: su dueño no lo permite.' : 'Ese video no existe o es privado.', true, 6000);
  const title = val('bvT'); if (title.length < 3) return toast('Escribe el título del concierto.', true);
  const synopsis = val('bvSy'); if (synopsis.length < 40) { $('bvSy').focus(); return toast('Escribe la sinopsis (mínimo 40 caracteres): de qué trata el concierto.', true, 5000); }
  const y = val('bvY'); if (y && !/^(19|20)\d{2}$/.test(y)) return toast('El año debe tener 4 cifras.', true);
  const d = val('bvD'); if (!/^\d{4}-\d{2}-\d{2}$/.test(d)) return toast('Elige la fecha en que entra en cartelera.', true);
  const n = Math.round(+val('bvN2') || 30); if (n < 1 || n > 365) return toast('Los días en cartelera van de 1 a 365.', true);
  const body = { title: title.slice(0, 140), synopsis: synopsis.slice(0, 1500), artist: val('bvA').slice(0, 100) || null, festival: val('bvF').slice(0, 100) || null, year: y ? +y : null, place: val('bvP').slice(0, 120) || null, provider: p.provider, video_id: p.video_id, source_name: val('bvS').slice(0, 100) || null, official: $('bvO').checked, note: $('bvN').value.trim().slice(0, 3000) || null, setlist: vaultSetParse($('bvL').value), cycle: val('bvC').slice(0, 60) || null, starts_on: d, days: n };
  const ed = rev.vEdit, btn = document.querySelector('[data-act=bvsave]'); if (btn) btn.disabled = true;
  try {
    if (revImg(rev.vCov)) body.cover = await revUploadPair(rev.vCov, 'boveda-' + revUid(), 1280, 640);
    else body.cover = revPic(rev.vCov) || null;
    const lv = ($('bvLive').value || '').trim(); if (!rev.v8off) body.live_at = lv ? new Date(lv).toISOString() : null;
    let id;
    if (ed) { await revFetch(`/rest/v1/vault?id=eq.${ed.id}`, { method: 'PATCH', body, prefer: 'return=minimal' }); id = ed.id; if (ed.cover && ed.cover !== body.cover) revUnPair(ed.cover); }
    else { const r = await revFetch('/rest/v1/vault', { method: 'POST', body: { ...body, created_by: revMe().uid }, prefer: 'return=representation' }); id = r && r[0] && r[0].id; }
    if (typeof vaultReset === 'function') vaultReset();
    toast(ed ? 'Cambios guardados.' : d <= revToday10() ? '¡Ya está en cartelera!' : `Programado: entra el ${vaultD(d, { day: 'numeric', month: 'long' })}.`);
    const from = rev.stack.pop(); if (from && from.view === 'admin') return revAdmin('vault');
    if (id) return revVaultOpen(id, true); return revVault();
  } catch (e) { if (btn) btn.disabled = false; if (!rev.v8off && /live_at|schema cache/i.test(e.message + (e.raw || ''))) { rev.v8off = true; toast('La función en vivo necesita el SQL nuevo; guardo el concierto sin ella.', true, 6000); return revVaultSave(); } toast(/actualizar la base/.test(e.message) ? V6_MSG : e.message, true, 7000); }
}
async function revVaultCurToggle(id, on) {
  if (!revIsAdmin() || !UUID.test(id)) return;
  if (!(await uiConfirm(on ? '¿Hacer curador de la Bóveda?' : '¿Quitar permiso de curador?', on ? 'Podrá programar y editar conciertos de la Bóveda del tiempo (borrar no, salvo que se lo actives en Admin › Bóveda). Puedes cambiar sus permisos cuando quieras.' : 'Ya no podrá programar conciertos en la Bóveda.', 'Confirmar', false))) return;
  if (on) await revFetch('/rest/v1/vault_curators', { method: 'POST', body: { user_id: id }, prefer: 'return=minimal' });
  else await revFetch(`/rest/v1/vault_curators?user_id=eq.${id}`, { method: 'DELETE' });
  await vaultCurators().catch(() => {}); toast(on ? 'Ahora es curador de la Bóveda.' : 'Permiso de curador quitado.');
  if (rev.view === 'profile') { revDrawProfile(); revProfTab(rev.profTab); }
  else if (rev.view === 'admin') revAdmin('vault');
}
async function revVaultAct(act, b, d) {
  const inAdm = rev.view === 'admin', v = (d.id && UUID.test(d.id) && (rev.admVRows || []).find(r => r.id === d.id)) || rev.vItem;
  const after = () => inAdm ? revAdmin('vault') : revVaultOpen(v.id, true);
  switch (act) {
    case 'bvtab': return revVault(d.v);
    case 'bvopen': return revVaultOpen(d.id);
    case 'bvplay': return vaultPlay(0);
    case 'bvseek': return vaultPlay(+d.v || 0);
    case 'bvcycle': rev.vCycle = d.v || ''; if (rev.view !== 'vault' || rev.vTab !== 'now') { rev.stack = []; rev.view = ''; } return revVault('now');
    case 'bvedit': return revVaultEdit(v);
    case 'bvnew': return revVaultEdit(null);
    case 'bvwatch': {
      if (!(await revNeed())) return; const me = revMe();
      try {
        if (rev.vWatch) await revFetch(`/rest/v1/vault_watch?vault_id=eq.${v.id}&user_id=eq.${me.uid}`, { method: 'DELETE' });
        else await revFetch('/rest/v1/vault_watch', { method: 'POST', body: { vault_id: v.id, user_id: me.uid }, prefer: 'return=minimal' });
        rev.vWatch = !rev.vWatch; b.classList.toggle('on', rev.vWatch); b.setAttribute('aria-pressed', String(rev.vWatch)); b.innerHTML = `${ICO('bell')} ${rev.vWatch ? 'Te avisaremos' : 'Avísame'}`;
        toast(rev.vWatch ? 'Te llegará un aviso en la revista cuando empiece.' : 'Aviso quitado.');
      } catch (e) { toast(/vault_watch|schema cache/i.test(e.message + (e.raw || '')) ? V6_MSG : e.message, true, 6000); }
      return;
    }
    case 'bvstory': return nvStory({ kicker: 'La bóveda del tiempo', title: v.title, sub: vaultMeta(v), img: vaultThumb(v), name: 'boveda-radio-de-ruta' });
    case 'bvperm': return vaultPerm(d.id, d.v, b.checked);
    case 'bvundo': return vaultUndo(+d.id);
    case 'bvlog': return vaultLogItem(v);
    case 'bvavt': rev.admVTab = d.v; return revAdmin('vault');
    case 'bvadmf': rev.admVF = d.v; return revAdmVaultDraw();
    case 'bvdiff': { const el = $('bvd' + d.id); if (el) el.hidden = !el.hidden; return; }
    case 'bvsave': return revVaultSave();
    case 'bvcheck': rev.vChk = null; return vaultCheck();
    case 'bvnext': $('bvD').value = vaultFree(rev.vRows || []); return;
    case 'bvpickcov': return $('bvCovFile').click();
    case 'bvnocov': rev.vCov = ''; return vaultDrawCov();
    case 'bvfeature': await revFetch(`/rest/v1/vault?id=eq.${v.id}`, { method: 'PATCH', body: { featured_on: v.featured_on === revToday10() ? null : revToday10() }, prefer: 'return=minimal' }); toast(v.featured_on === revToday10() ? 'Ya no está fijado.' : 'Hoy es el concierto del día.'); return after();
    case 'bvhide': await revFetch(`/rest/v1/vault?id=eq.${v.id}`, { method: 'PATCH', body: { hidden: !v.hidden }, prefer: 'return=minimal' }); if (typeof vaultReset === 'function') vaultReset(); toast(v.hidden ? 'Visible otra vez.' : 'Oculto: solo lo ven los curadores.'); return after();
    case 'bvdel': if (!(await uiConfirm('¿Borrar este concierto de la Bóveda?', 'Se borran también sus comentarios. No se puede deshacer.', 'Borrar'))) return; await revFetch(`/rest/v1/vault?id=eq.${v.id}`, { method: 'DELETE' }); revUnPair(v.cover); if (typeof vaultReset === 'function') vaultReset(); toast('Concierto borrado. Queda una copia en el historial por si quieres recuperarlo.', false, 5000); if (inAdm) return revAdmin('vault'); rev.stack = []; return revVault();
    case 'bvresched': { const nv = { ...v, starts_on: vaultFree(rev.vRows || []), featured_on: null }; delete nv.ends_on; return revVaultEdit(nv); }
    case 'bvcom': {
      if (!(await revNeed())) return; const t = ($('bvCom').value || '').trim(); if (!t) return;
      await revFetch('/rest/v1/vault_comments', { method: 'POST', body: { vault_id: v.id, author_id: revMe().uid, body: t.slice(0, 1000) }, prefer: 'return=minimal' });
      $('bvCom').value = ''; return vaultComsRefresh();
    }
    case 'bvdelc': if (!(await uiConfirm('¿Borrar comentario?', 'No se puede deshacer.', 'Borrar'))) return; await revFetch(`/rest/v1/vault_comments?id=eq.${d.id}`, { method: 'DELETE' }); rev.vComs = rev.vComs.filter(c => c.id !== d.id); return revVaultDraw();
    case 'bvshare': return revShare(v.title + ' · La bóveda del tiempo', '#rvb=' + v.id);
    case 'bvplaylib': { const n = revNk(v.artist); return playLib(r => revNk(r.artist) === n); }
    case 'bvart': return rev.vArt ? revArtist(rev.vArt.id) : null;
    case 'bvcur': return revVaultCurToggle(d.id, d.v === '1');
  }
}
/* --- Edición y permisos: panel del admin (Admin › Bóveda) e historial de cambios --- */
const VAULT_F = { title: 'título', synopsis: 'sinopsis', artist: 'artista', festival: 'festival', year: 'año', place: 'lugar', provider: 'plataforma', video_id: 'video', source_name: 'fuente', official: 'canal oficial', note: 'nota del curador', setlist: 'lista de canciones', cycle: 'ciclo', cover: 'imagen', starts_on: 'fecha de entrada', days: 'días en cartelera', featured_on: 'concierto del día', hidden: 'visibilidad' };
const VAULT_NOUNDO = new Set(['id', 'created_at', 'created_by', 'ends_on']);
const vaultVal = (k, x) => x == null || x === '' ? '—' : k === 'official' ? (x ? 'sí' : 'no') : k === 'hidden' ? (x ? 'oculto' : 'visible') : k === 'setlist' ? (Array.isArray(x) ? `${x.length} canciones` : '—') : k === 'video_id' || k === 'cover' ? 'cambiado' : String(x).length > 160 ? String(x).slice(0, 160) + '…' : String(x);
function vaultLogRow(l, withTitle) {
  const who = l.profiles ? esc(l.profiles.name) : 'Alguien', ks = Object.keys(l.changes || {}).filter(k => VAULT_F[k]);
  const what = l.action === 'create' ? 'programó' : l.action === 'delete' ? 'borró' : 'editó';
  const det = l.action === 'edit' && ks.length ? `<div class="bv-diff" id="bvd${l.id}" hidden>${ks.map(k => `<p><b>${esc(VAULT_F[k])}:</b> <s>${esc(vaultVal(k, l.changes[k][0]))}</s> → ${esc(vaultVal(k, l.changes[k][1]))}</p>`).join('')}</div>` : '';
  const undo = revIsAdmin() && (l.action === 'delete' || (l.action === 'edit' && ks.length)) ? `<button type="button" class="rv-x" data-act="bvundo" data-id="${l.id}">${l.action === 'delete' ? 'Recuperar' : 'Deshacer'}</button>` : '';
  return `<div class="bv-lrow"><p><b>${who}</b> ${what} ${withTitle ? `«${esc(l.title || '')}»` : ''}${ks.length && l.action === 'edit' ? ` <span class="rv-m">(${ks.map(k => esc(VAULT_F[k])).join(', ')})</span>` : ''}</p><p class="rv-m">${revDate(l.created_at, { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}${det ? ` · <button type="button" class="rv-x" data-act="bvdiff" data-id="${l.id}">ver cambios</button>` : ''} ${undo}</p>${det}</div>`;
}
async function vaultLogItem(v) {
  const el = $('bvLog'); if (!el || !v) return; if (el.innerHTML) { el.innerHTML = ''; return; }
  el.innerHTML = '<p class="hint">Cargando historial…</p>';
  try {
    rev.vLog = await revFetch(`/rest/v1/vault_log?select=*,profiles!actor(name)&vault_id=eq.${v.id}&order=created_at.desc&limit=50`);
    el.innerHTML = `<div class="bv-log">${rev.vLog.map(l => vaultLogRow(l, false)).join('') || '<p class="hint">Sin cambios registrados.</p>'}</div>`;
  } catch (e) { el.innerHTML = `<p class="hint">${esc(/actualizar la base/.test(e.message) ? V6_MSG : e.message)}</p>`; }
}
async function vaultUndo(id) {
  if (!revIsAdmin()) return;
  const l = [...(rev.vLog || []), ...(rev.admVLog || [])].find(x => x.id === id); if (!l) return;
  if (l.action === 'delete') {
    if (!(await uiConfirm('¿Recuperar este concierto?', `«${l.title}» vuelve a la Bóveda con todos sus datos (sus comentarios no se recuperan).`, 'Recuperar', false))) return;
    const body = {}; Object.keys(l.changes || {}).forEach(k => { if (k !== 'ends_on' && k !== 'created_at') body[k] = l.changes[k]; }); body.created_by = revMe().uid;
    await revFetch('/rest/v1/vault', { method: 'POST', body, prefer: 'return=minimal' });
    toast('Concierto recuperado.');
  } else {
    const ks = Object.keys(l.changes || {}).filter(k => VAULT_F[k] && !VAULT_NOUNDO.has(k)); if (!ks.length) return;
    if (!(await uiConfirm('¿Deshacer este cambio?', `Vuelve a como estaba: ${ks.map(k => VAULT_F[k]).join(', ')}.`, 'Deshacer', false))) return;
    const body = {}; ks.forEach(k => { body[k] = l.changes[k][0]; });
    const r = await revFetch(`/rest/v1/vault?id=eq.${l.vault_id}`, { method: 'PATCH', body, prefer: 'return=representation' });
    if (!r || !r.length) return toast('Ese concierto ya no existe; recupéralo primero desde su borrado.', true, 5000);
    toast('Cambio deshecho.');
  }
  if (typeof vaultReset === 'function') vaultReset();
  if (rev.view === 'admin') return revAdmin('vault');
  return revVaultOpen(l.vault_id, true);
}
async function vaultPerm(uid, what, on) {
  if (!revIsAdmin() || !UUID.test(uid) || !['create', 'edit', 'delete'].includes(what)) return;
  try { await revFetch(`/rest/v1/vault_curators?user_id=eq.${uid}`, { method: 'PATCH', body: { ['can_' + what]: !!on }, prefer: 'return=minimal' }); await vaultCurators(); toast('Permiso guardado.'); }
  catch (e) { toast(/actualizar la base/.test(e.message) ? V6_MSG : e.message, true, 6000); }
  revAdmVaultDraw();
}
async function revAdmVault() {
  const my = rev.req, box = revAdmBox(); if (!box) return;
  rev.admVTab = rev.admVTab || 'cartelera';
  const t = (v, n) => `<button type="button" class="rv-tab${rev.admVTab === v ? ' on' : ''}" data-act="bvavt" data-v="${v}">${n}</button>`;
  box.innerHTML = `<div class="rv-tabs bv-atabs">${t('cartelera', 'Cartelera')}${t('permisos', 'Quién puede editar')}${t('historial', 'Historial')}</div><div id="bvAdm"><p class="hint">Cargando…</p></div>`;
  try {
    const [rows, , log] = await Promise.all([
      rev.admVTab === 'cartelera' ? revFetch('/rest/v1/vault?select=*&order=starts_on.desc&limit=300') : [],
      vaultCurators(),
      rev.admVTab === 'historial' ? revFetch('/rest/v1/vault_log?select=*,profiles!actor(name)&order=created_at.desc&limit=80') : []]);
    if (my !== rev.req) return;
    rev.admVRows = rows; rev.vRows = rows.length ? rows : rev.vRows; rev.admVLog = log; revAdmVaultDraw();
    if (rev.admVTab === 'permisos') vaultUserSearch();
  } catch (e) { if (my === rev.req && $('bvAdm')) $('bvAdm').innerHTML = `<p class="hint">${esc(/actualizar la base|vault_log|can_create/.test(e.message) ? V6_MSG : e.message)}</p>`; }
}
function revAdmVaultDraw() {
  const el = $('bvAdm'); if (!el) return;
  if (rev.admVTab === 'permisos') {
    const ck = (c, w, n) => `<label class="chk"><input type="checkbox" data-act="bvperm" data-id="${esc(c.user_id)}" data-v="${w}"${c['can_' + w] ? ' checked' : ''}> ${n}</label>`;
    el.innerHTML = `<p class="bv-rule">Solo tú, con tu cuenta de administrador, puedes dar o quitar estos permisos. La base de datos lo comprueba: aunque alguien modificara la app, no podría saltárselo. Tú siempre puedes todo.</p>
<h4 class="rv-h">Curadores (${rev.curList.length})</h4>${rev.curList.map(c => `<div class="rv-urow bv-crow">${revAv(c.profiles, 40)}<div><b>${esc(c.profiles ? c.profiles.name : 'Usuario')}</b><div class="bv-perms">${ck(c, 'create', 'Programar')}${ck(c, 'edit', 'Editar')}${ck(c, 'delete', 'Borrar')}</div></div><div class="rv-pcb"><button type="button" class="alt" data-act="bvcur" data-id="${esc(c.user_id)}" data-v="0">Quitar</button></div></div>`).join('') || '<p class="hint">Todavía no hay curadores aparte de ti.</p>'}
<h4 class="rv-h">Añadir curador</h4><input id="bvUq" class="rv-search" type="search" placeholder="Buscar usuario por nombre…" autocomplete="off" value="${esc(rev.bvUq || '')}"><div id="bvUl"></div>
<p class="rv-m">Al añadir a alguien empieza con permiso para programar y editar, pero no para borrar. Cada cambio queda en el historial.</p>`;
    const q = $('bvUq'); let tm; q.oninput = () => { clearTimeout(tm); tm = setTimeout(() => { rev.bvUq = q.value; vaultUserSearch(); }, 350); };
    return;
  }
  if (rev.admVTab === 'historial') {
    el.innerHTML = `<p class="rv-m">Todo lo que se programa, edita o borra en la Bóveda queda registrado aquí por la base de datos (nadie puede borrarlo ni falsearlo). Puedes deshacer un cambio o recuperar un concierto borrado.</p><div class="bv-log">${(rev.admVLog || []).map(l => vaultLogRow(l, true)).join('') || '<p class="hint">Sin cambios todavía.</p>'}</div>`;
    return;
  }
  const rows = rev.admVRows || [], f = rev.admVF || 'all';
  const st = v => v.hidden ? 'hidden' : vaultState(v);
  const lab = { now: 'En cartelera', soon: 'Próximamente', past: 'Ya pasó', hidden: 'Oculto' };
  const n = k => k === 'all' ? rows.length : k === 'nosyn' ? rows.filter(v => !v.synopsis).length : rows.filter(v => st(v) === k).length;
  const chip = (k, t) => `<button type="button" class="rv-tab${f === k ? ' on' : ''}" data-act="bvadmf" data-v="${k}">${t} · ${n(k)}</button>`;
  const list = rows.filter(v => f === 'all' || (f === 'nosyn' ? !v.synopsis : st(v) === f));
  el.innerHTML = `<div class="bv-cycles">${chip('all', 'Todos')}${chip('now', 'En cartelera')}${chip('soon', 'Próximamente')}${chip('past', 'Ya pasaron')}${chip('hidden', 'Ocultos')}${chip('nosyn', 'Sin sinopsis')}</div>
<div class="row" style="margin:6px 0 12px"><button type="button" data-act="bvnew">+ Programar concierto</button></div>
${list.map(v => `<div class="bv-arow"><button type="button" class="bv-athumb" data-act="bvopen" data-id="${esc(v.id)}">${vaultImg(v)}</button><div class="bv-ainfo"><b>${esc(v.title)}</b><small><span class="bv-st bv-st-${st(v)}">${lab[st(v)]}</span> ${esc(vaultD(v.starts_on))} → ${esc(vaultD(vaultAdd(v.ends_on, -1)))} · ${v.days} días${v.featured_on === revToday10() ? ' · <b>concierto del día</b>' : ''}</small>${v.synopsis ? `<small class="bv-syn">${esc(v.synopsis)}</small>` : '<small class="bv-warn">Falta sinopsis</small>'}</div><div class="rv-pcb"><button type="button" class="alt" data-act="bvedit" data-id="${esc(v.id)}">Editar</button><button type="button" class="alt" data-act="bvhide" data-id="${esc(v.id)}">${v.hidden ? 'Mostrar' : 'Ocultar'}</button><button type="button" class="alt" data-act="bvdel" data-id="${esc(v.id)}">Borrar</button></div></div>`).join('') || '<p class="hint">No hay conciertos en este filtro.</p>'}`;
}
async function vaultUserSearch() {
  const el = $('bvUl'); if (!el) return; const t = revPat(rev.bvUq || '');
  if (!t) { el.innerHTML = ''; return; }
  try {
    const a = await revFetch(`/rest/v1/profiles?select=id,name,avatar,role,banned&name=ilike.${encodeURIComponent('*' + t + '*')}&limit=20`);
    if (!$('bvUl')) return;
    $('bvUl').innerHTML = a.filter(u => u.role !== 'admin' && !u.banned).map(u => `<div class="rv-urow">${revAv(u, 36)}<div><b>${esc(u.name)}</b></div><div class="rv-pcb">${rev.curators.has(u.id) ? '<span class="rv-m">Ya es curador</span>' : `<button type="button" class="alt" data-act="bvcur" data-id="${esc(u.id)}" data-v="1">Hacer curador</button>`}</div></div>`).join('') || '<p class="hint">Sin resultados.</p>';
  } catch (e) { el.innerHTML = `<p class="hint">${esc(e.message)}</p>`; }
}

/* Función en vivo: cuenta regresiva, todos ven el mismo momento y el chat se actualiza solo */
const VAULT_LIVE_H = 3;
const vaultLive = v => { if (!v || !v.live_at) return ''; const t = Date.parse(v.live_at), n = Date.now(); return n < t ? 'pre' : n < t + VAULT_LIVE_H * 3600e3 ? 'live' : 'done'; };
const vaultLocalDT = s => { if (!s) return ''; const d = new Date(s); if (isNaN(d)) return ''; const p = n => String(n).padStart(2, '0'); return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`; };
const vaultCount = ms => { const s = Math.max(0, Math.floor(ms / 1000)), d = Math.floor(s / 86400), h = Math.floor(s % 86400 / 3600), m = Math.floor(s % 3600 / 60), x = s % 60; return d ? `${d} d ${h} h ${m} min` : h ? `${h} h ${m} min ${x} s` : `${m} min ${x} s`; };
function vaultLiveHtml(v) {
  const k = vaultLive(v); if (!k || k === 'done') return '';
  const when = new Date(v.live_at).toLocaleString('es', { weekday: 'long', day: 'numeric', month: 'long', hour: 'numeric', minute: '2-digit' });
  return k === 'live' ? `<div class="bv-live on"><span class="bv-dot"></span><b>EN VIVO AHORA</b><span>Dale play: todos ven el mismo momento y comentan en el chat.</span></div>`
    : `<div class="bv-live"><span class="bv-dot"></span><b>Función en vivo</b><span>${esc(when)} · empieza en <b id="bvCd">${vaultCount(Date.parse(v.live_at) - Date.now())}</b></span></div>`;
}
async function vaultComsRefresh() {
  const v = rev.vItem, el = $('bvComs'); if (!v || !el) return;
  try {
    const cm = await revFetch(`/rest/v1/vault_comments?select=*,profiles!author_id(name,avatar)&vault_id=eq.${v.id}&order=created_at.asc&limit=300`);
    if (rev.vItem !== v || !$('bvComs')) return; rev.vComs = cm;
    const atEnd = el.getBoundingClientRect().bottom < innerHeight + 80;
    el.innerHTML = cm.map(rev.vCm).join('') || '<p class="hint">¿Lo viste? Cuenta qué te pareció.</p>';
    const h = $('bvComH'); if (h) h.textContent = `${vaultLive(v) === 'live' ? '🔴 Chat en vivo' : 'Comentarios'} (${cm.length})`;
    if (atEnd && vaultLive(v) === 'live') el.lastElementChild && el.lastElementChild.scrollIntoView({ block: 'nearest' });
  } catch {}
}
function vaultLiveLoop(id) {
  clearInterval(rev.vLiveT); let n = 0;
  rev.vLiveT = setInterval(() => {
    const v = rev.vItem; if (!v || v.id !== id || rev.view !== 'vitem' || !$('bvComs')) { clearInterval(rev.vLiveT); return; }
    const k = vaultLive(v); if (!k || k === 'done') { clearInterval(rev.vLiveT); return; }
    if (k === 'pre') { const c = $('bvCd'); if (c) c.textContent = vaultCount(Date.parse(v.live_at) - Date.now()); if (Date.now() >= Date.parse(v.live_at)) revVaultDraw(); return; }
    if (++n % 6 === 0 && !document.hidden) vaultComsRefresh(); // chat: cada 6 segundos mientras dura la función
  }, 1000);
}

/* --- Revista viva: barra de secciones, menú «Más», portada de inicio, perfiles con fondo y muro --- */
const NV_ACTS = new Set(['nvmore', 'nvclose', 'nvwpost', 'nvwimg', 'nvwnoimg', 'nvwlike', 'nvwdel', 'nvgocom', 'nvpickban', 'nvnoban', 'nvlegal', 'nvwpoll', 'nvwded', 'nvwrep', 'nvwreply', 'nvwvote', 'nvat', 'nvwstory', 'nvnowplay']);
const V7_MSG = 'Falta activar los muros y las portadas de perfil: ejecuta otra vez revista_supabase_completo.sql en Supabase (SQL Editor). No borra nada.';
(() => { try { if (!document.getElementById('nvFont')) { const l = document.createElement('link'); l.id = 'nvFont'; l.rel = 'stylesheet'; l.href = 'https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:opsz,wght@12..96,600;12..96,800&display=swap'; document.head.appendChild(l); } } catch {} })();
const nvHue = s => [...String(s || '?')].reduce((a, c) => (a * 31 + c.charCodeAt(0)) % 360, 7);
/* Barra de secciones (tipo app) */
function nvDock() {
  const it = (v, ic, n, act = 'sec') => `<button type="button" class="nv-di${rev.sec === v || (v === 'more' && (rev.sec === 'artists' || rev.sec === 'club' || rev.sec === 'more')) ? ' on' : ''}" data-act="${act}" data-v="${v}">${ICO(ic)}<span>${n}</span></button>`;
  return `<nav class="nv-dock" aria-label="Secciones de la revista">${it('art', 'home', 'Inicio')}${it('vault', 'film', 'Bóveda')}${it('posters', 'image', 'Pósters')}${it('agenda', 'calendar', 'Agenda')}${revIsAdmin() ? it('admin', 'wrench', 'Admin') : ''}${it('more', 'menu', 'Más', 'nvmore')}</nav>`;
}
function nvMore() {
  const me = revMe(), old = $('nvSheet'); if (old) { old.remove(); return; }
  const t = (act, ic, n, extra = '') => `<button type="button" class="nv-tile" data-act="${act}"${extra}>${ICO(ic)}<span>${n}</span></button>`;
  const h = `<div class="nv-sheet" id="nvSheet"><div class="nv-shbg" data-act="nvclose"></div><div class="nv-shc" role="dialog" aria-modal="true" aria-label="Más de la revista"><div class="nv-grab"></div>
<p class="nv-shk">Más de la revista</p><div class="nv-tiles">
${me ? t('profile', 'user', 'Mi perfil', ` data-id="${esc(me.uid)}"`) : t('acct', 'user', 'Entrar')}${t('sec', 'music', 'Club de escucha', ' data-v="club"')}${t('lsindex', 'shuffle', 'Listas')}${t('svlist', 'download', 'Guardados')}${t('sec', 'user', 'Artistas', ' data-v="artists"')}${t('notifs', 'bell', 'Avisos')}${t('search', 'search', 'Buscar')}${me ? t('tab', 'pencil', 'Mis artículos', ' data-v="mine"') : ''}${t('tab', 'news', 'Debates', ' data-v="polls"')}${t('tab', 'book', 'Ediciones', ' data-v="editions"')}${t('rules', 'sparkles', 'Normas')}${t('nvlegal', 'activity', 'Aviso legal')}
</div><button type="button" class="alt nv-shx" data-act="nvclose">Cerrar</button></div></div>`;
  $('view-revista').insertAdjacentHTML('beforeend', h);
  const f = document.querySelector('#nvSheet .nv-tile'); if (f) f.focus({ preventScroll: true });
}
const nvCloseSheet = () => { const s = $('nvSheet'); if (s) s.remove(); };
document.addEventListener('keydown', e => { if (e.key === 'Escape' && $('nvSheet')) nvCloseSheet(); });
/* Portada de inicio: saludo, concierto del día y pósters recientes */
function nvHello() {
  const h = new Date().getHours(), me = rev.me || revMe();
  const s = h < 5 ? 'Buenas noches' : h < 12 ? 'Buenos días' : h < 19 ? 'Buenas tardes' : 'Buenas noches';
  return `<header class="nv-hello"><p class="nv-day">${esc(revDate(Date.now(), { weekday: 'long', day: 'numeric', month: 'long' }))}</p><h2>${s}${me && me.name ? `, <span>${esc(me.name)}</span>` : ''}</h2><p>Lo nuevo en tu revista de música.</p></header>`;
}
async function nvHomeExtras(out) {
  const t = revToday10();
  await Promise.allSettled([
    revFetch(`/rest/v1/vault?select=id,title,artist,festival,year,place,provider,video_id,cover,starts_on,ends_on,featured_on,created_at&starts_on=lte.${t}&ends_on=gt.${t}&hidden=eq.false&limit=60`).then(a => { out.vday = typeof vaultOfDay === 'function' ? vaultOfDay(a) : null; }),
    revFetch('/rest/v1/posters?select=id,title,artist,year,kind,thumb_url,image_url&order=created_at.desc&limit=12').then(a => { out.posters = a; }),
    revFetch(`/rest/v1/clubs?select=id,album,artist,year,cover,starts_on,ends_on&starts_on=lte.${t}&ends_on=gt.${t}&order=starts_on.desc&limit=1`).then(a => { out.club = a[0] || null; })
  ]);
}
function nvVaultBanner(v) {
  if (!v) return '';
  return `<button type="button" class="nv-vb" data-act="bvopen" data-id="${esc(v.id)}"><span class="nv-vbimg">${vaultImg(v)}</span><span class="nv-vbtx"><span class="nv-vbk">${ICO('film')} Hoy en la Bóveda</span><b>${esc(v.title)}</b><small>${esc(vaultMeta(v))}</small><span class="nv-vbgo">${ICO('play')} Ver concierto</span></span></button>`;
}
function nvPosterRow(list) {
  if (!list || !list.length) return '';
  return `<section class="nv-row"><div class="nv-rowh"><h4>Pósters de la comunidad</h4><button type="button" class="nv-more" data-act="sec" data-v="posters">Ver todos ›</button></div><div class="nv-hscroll">${list.map(p => { const src = revUrl(p.thumb_url) || revUrl(p.image_url); return src ? `<button type="button" class="nv-pst" data-act="poster" data-id="${esc(p.id)}"><img src="${esc(src)}" alt="" loading="lazy" decoding="async"><b>${esc(p.title)}</b><small>${esc([p.artist, p.year].filter(Boolean).join(' · '))}</small></button>` : ''; }).join('')}</div></section>`;
}
/* Muro del perfil: notas, respuestas, encuestas, dedicatorias, menciones y etiquetas */
const V8_MSG = 'Falta activar respuestas, encuestas y dedicatorias: ejecuta otra vez revista_supabase_completo.sql en Supabase (SQL Editor). No borra nada.';
const nvV8err = e => /parent_id|dedication|poll|wall_votes|schema cache|column/i.test((e && (e.message + (e.raw || ''))) || '');
const nvRich = t => esc(t)
  .replace(/(^|[\s(¡¿])@([\p{L}\p{N}_.-]{2,40})/gu, (m, a, n) => `${a}<a href="#" class="nv-at" data-act="nvat" data-v="${n}">@${n}</a>`)
  .replace(/(^|[\s(¡¿])#([\p{L}\p{N}_-]{2,40})/gu, (m, a, n) => `${a}<a href="#" class="nv-hash" data-act="tag" data-v="${n.toLowerCase()}">#${n}</a>`)
  .replace(/\n/g, '<br>');
async function nvWall() {
  const p = rev.prof, me = revMe(), my = ++rev.req, el = $('rvPList'); if (!p || !el) return;
  el.innerHTML = '<p class="hint">Cargando el muro…</p>';
  try {
    const a = await revFetch(`/rest/v1/wall_posts?select=*,profiles!author_id(name,avatar),wall_likes(count)&owner_id=eq.${p.id}&order=created_at.desc&limit=80`);
    let liked = new Set(), votes = [];
    const ids = a.map(x => x.id).join(','), polls = a.filter(x => Array.isArray(x.poll)).map(x => x.id).join(',');
    await Promise.all([
      me && a.length ? revFetch(`/rest/v1/wall_likes?select=post_id&user_id=eq.${me.uid}&post_id=in.(${ids})`).then(l => { liked = new Set(l.map(x => x.post_id)); }) : null,
      polls ? revFetch(`/rest/v1/wall_votes?select=post_id,option,user_id&post_id=in.(${polls})`).then(v => { votes = v; }).catch(() => {}) : null]);
    if (my !== rev.req || !$('rvPList')) return;
    rev.wall = a; rev.wallLiked = liked; rev.wallVotes = votes; rev.nvWImg = ''; rev.nvPollOn = false; rev.nvDed = false; nvDrawWall();
  } catch (e) { if (my === rev.req && $('rvPList')) $('rvPList').innerHTML = `<p class="hint">${esc(/actualizar la base|wall_|schema cache|does not exist/i.test(e.message + (e.raw || '')) ? V7_MSG : e.message)}</p>`; }
}
function nvPollHtml(w) {
  const me = revMe(), vs = (rev.wallVotes || []).filter(v => v.post_id === w.id), tot = vs.length, mine = me && vs.find(v => v.user_id === me.uid);
  return `<div class="nv-poll">${w.poll.map((o, i) => { const n = vs.filter(v => v.option === i).length, pc = tot ? Math.round(n * 100 / tot) : 0; return `<button type="button" class="nv-popt${mine && mine.option === i ? ' on' : ''}" data-act="nvwvote" data-id="${esc(w.id)}" data-v="${i}"><span class="nv-pbar" style="width:${mine ? pc : 0}%"></span><span class="nv-ptx">${esc(o)}</span>${mine ? `<b>${pc}%</b>` : ''}</button>`; }).join('')}<p class="rv-m">${tot} ${tot === 1 ? 'voto' : 'votos'}${mine ? ' · toca otra opción para cambiar tu voto' : ' · vota para ver los resultados'}</p></div>`;
}
function nvDrawWall() {
  const p = rev.prof, me = revMe(), mine = me && me.uid === p.id, el = $('rvPList'); if (!el) return;
  const can = me && !revBanned() && (mine || p.wall_open !== false);
  const comp = can ? `<div class="nv-comp">${revAv(rev.me || { name: me.name }, 40)}<div class="nv-compb"><textarea id="nvWT" rows="2" maxlength="1000" placeholder="${mine ? '¿Qué estás escuchando hoy?' : `Escribe algo en el muro de ${esc(p.name)}…`}"></textarea>
<div id="nvPoll" class="nv-pin"${rev.nvPollOn ? '' : ' hidden'}>${[0, 1, 2, 3].map(i => `<input id="nvPO${i}" maxlength="60" placeholder="Opción ${i + 1}${i > 1 ? ' (opcional)' : ''}">`).join('')}</div>
<div id="nvWImg" class="nv-wimg"></div>
<div class="nv-compa"><button type="button" class="nv-ib" data-act="nvwimg" aria-label="Añadir una imagen">${ICO('image')}</button><button type="button" class="nv-ib${rev.nvPollOn ? ' on' : ''}" data-act="nvwpoll" aria-label="Encuesta" aria-pressed="${rev.nvPollOn}">${ICO('activity')}</button>${mine ? '' : `<button type="button" class="nv-ib${rev.nvDed ? ' on' : ''}" data-act="nvwded" aria-label="Dedicatoria para la radio" aria-pressed="${rev.nvDed}">${ICO('mic')}</button>`}<span class="rv-m" id="nvWN">0/1000</span><button type="button" data-act="nvwpost">Publicar</button></div>
${rev.nvDed ? `<p class="nv-dedhint">${ICO('mic')} Dedicatoria: Luna la leerá al aire en la radio de ${esc(p.name)}.</p>` : ''}${rev.nvPollOn ? '<p class="nv-dedhint">Escribe la pregunta arriba y de 2 a 4 opciones.</p>' : ''}</div><input type="file" id="nvWFile" accept="image/*" hidden></div>`
    : !me ? '<p class="hint nv-wnote">Entra a tu cuenta para escribir en los muros.</p>' : !mine && p.wall_open === false ? `<p class="hint nv-wnote">${esc(p.name)} tiene su muro solo para sus propias notas.</p>` : '';
  const head = (w, n) => { const del = me && (me.uid === w.author_id || me.uid === w.owner_id || revIsAdmin()); return `<div class="nv-nh"><a href="#" data-act="profile" data-id="${esc(w.author_id)}">${revAv(w.profiles, n)}</a><div class="nv-nwho"><a href="#" data-act="profile" data-id="${esc(w.author_id)}"><b>${esc((w.profiles && w.profiles.name) || 'Alguien')}</b></a>${!w.parent_id && w.author_id !== w.owner_id ? `<span class="nv-to">› en el muro de ${esc(p.name)}</span>` : ''}<small>${revAgo(w.created_at)}</small></div>${del ? `<button type="button" class="nv-ib" data-act="nvwdel" data-id="${esc(w.id)}" aria-label="Borrar">${ICO('trash')}</button>` : ''}</div>`; };
  const like = w => { const on = rev.wallLiked.has(w.id); return `<button type="button" class="nv-lk${on ? ' on' : ''}" data-act="nvwlike" data-id="${esc(w.id)}" aria-pressed="${on}">${ICO('heart')} <span>${revCnt(w.wall_likes)}</span></button>`; };
  const reply = w => `<div class="nv-reply">${head(w, 30)}<p class="nv-nb">${nvRich(w.body)}</p><div class="nv-nacts">${like(w)}${me && me.uid !== w.author_id ? `<button type="button" class="rv-x" data-act="report" data-type="wall" data-id="${esc(w.id)}">Denunciar</button>` : ''}</div></div>`;
  const note = w => {
    const im = revPic(w.image), ks = rev.wall.filter(x => x.parent_id === w.id).sort((a, b) => String(a.created_at).localeCompare(String(b.created_at)));
    return `<article class="nv-note${w.dedication ? ' nv-isded' : ''}">${w.dedication ? `<p class="nv-ded">${ICO('mic')} Dedicatoria para la radio de ${esc(p.name)}</p>` : ''}${head(w, 42)}
<p class="nv-nb">${nvRich(w.body)}</p>${Array.isArray(w.poll) ? nvPollHtml(w) : ''}${im ? `<img class="nv-nimg" src="${esc(im)}" alt="" loading="lazy" decoding="async">` : ''}
<div class="nv-nacts">${like(w)}${can || (me && !revBanned() && (mine || p.wall_open !== false)) ? `<button type="button" class="nv-lk" data-act="nvwrep" data-id="${esc(w.id)}">${ICO('chat')} <span>${ks.length || 'Responder'}</span></button>` : ks.length ? `<span class="nv-lk">${ICO('chat')} <span>${ks.length}</span></span>` : ''}<button type="button" class="nv-lk" data-act="nvwstory" data-id="${esc(w.id)}" aria-label="Imagen para historias">${ICO('share')}</button>${me && me.uid !== w.author_id ? `<button type="button" class="rv-x" data-act="report" data-type="wall" data-id="${esc(w.id)}">Denunciar</button>` : ''}</div>
${ks.length ? `<div class="nv-replies">${ks.map(reply).join('')}</div>` : ''}<div class="nv-rcomp" id="nvRC-${esc(w.id)}" hidden><textarea rows="2" maxlength="1000" placeholder="Escribe una respuesta…"></textarea><button type="button" data-act="nvwreply" data-id="${esc(w.id)}">Responder</button></div></article>`;
  };
  const roots = rev.wall.filter(w => !w.parent_id);
  el.innerHTML = `<div class="nv-wall">${comp}${roots.map(note).join('') || `<div class="nv-empty">${ICO('sparkles')}<p>${mine ? 'Tu muro está vacío. Cuenta qué estás escuchando, comparte una foto de tu colección o un recuerdo de concierto.' : `${esc(p.name)} todavía no tiene notas en su muro.`}</p></div>`}</div>`;
  const ta = $('nvWT'); if (ta) ta.addEventListener('input', () => { $('nvWN').textContent = `${ta.value.length}/1000`; ta.style.height = 'auto'; ta.style.height = Math.min(260, ta.scrollHeight) + 'px'; });
  const f = $('nvWFile'); if (f) f.onchange = async e => { const x = e.target.files && e.target.files[0]; e.target.value = ''; if (!x) return; try { rev.nvWImg = (await revResize(x, 1280, 0.84)).data(); nvDrawWImg(); } catch { toast('No se pudo leer esa imagen.', true); } };
  nvDrawWImg();
}
const nvDrawWImg = () => { const el = $('nvWImg'); if (el) el.innerHTML = rev.nvWImg ? `<div class="nv-wprev"><img src="${esc(rev.nvWImg)}" alt=""><button type="button" class="nv-ib" data-act="nvwnoimg" aria-label="Quitar imagen">${ICO('x')}</button></div>` : ''; };
function nvKeepDraft(fn) { const t = $('nvWT') ? $('nvWT').value : '', o = [0, 1, 2, 3].map(i => $('nvPO' + i) ? $('nvPO' + i).value : ''); fn(); nvDrawWall(); if ($('nvWT')) { $('nvWT').value = t; $('nvWT').dispatchEvent(new Event('input')); } o.forEach((v, i) => { if ($('nvPO' + i)) $('nvPO' + i).value = v; }); }
async function nvWallPost(btn) {
  if (!(await revNeed())) return; const p = rev.prof, me = revMe(), t = ($('nvWT').value || '').trim();
  if (!t) return toast(rev.nvPollOn ? 'Escribe la pregunta de la encuesta.' : 'Escribe algo para publicar en el muro.', true);
  const body = { owner_id: p.id, author_id: me.uid, body: t.slice(0, 1000) };
  if (rev.nvPollOn) { const o = [0, 1, 2, 3].map(i => ($('nvPO' + i).value || '').trim().slice(0, 60)).filter(Boolean); if (o.length < 2) return toast('Escribe al menos 2 opciones para la encuesta.', true); body.poll = o; }
  if (rev.nvDed && me.uid !== p.id) body.dedication = true;
  if (btn) btn.disabled = true;
  try {
    if (rev.nvWImg) body.image = await revUploadPair(rev.nvWImg, 'muro-' + revUid(), 1280, 480);
    await revFetch('/rest/v1/wall_posts', { method: 'POST', body, prefer: 'return=minimal' });
    toast(body.dedication ? 'Dedicatoria enviada: Luna la leerá en su radio.' : 'Publicado en el muro.'); return nvWall();
  } catch (e) { if (btn) btn.disabled = false; toast(nvV8err(e) && (body.poll || body.dedication) ? V8_MSG : /actualizar la base|wall_|schema cache/i.test(e.message + (e.raw || '')) ? V7_MSG : e.message, true, 6000); }
}
async function nvWallReply(id, btn) {
  if (!(await revNeed())) return; const box = $('nvRC-' + id), ta = box && box.querySelector('textarea'), t = ((ta && ta.value) || '').trim(); if (!t) return;
  if (btn) btn.disabled = true;
  try { await revFetch('/rest/v1/wall_posts', { method: 'POST', body: { owner_id: rev.prof.id, author_id: revMe().uid, body: t.slice(0, 1000), parent_id: id }, prefer: 'return=minimal' }); return nvWall(); }
  catch (e) { if (btn) btn.disabled = false; toast(nvV8err(e) ? V8_MSG : e.message, true, 6000); }
}
async function nvWallVote(id, opt) {
  if (!(await revNeed())) return; const me = revMe();
  try {
    await revFetch(`/rest/v1/wall_votes?post_id=eq.${id}&user_id=eq.${me.uid}`, { method: 'DELETE' });
    await revFetch('/rest/v1/wall_votes', { method: 'POST', body: { post_id: id, user_id: me.uid, option: opt }, prefer: 'return=minimal' });
    rev.wallVotes = (rev.wallVotes || []).filter(v => !(v.post_id === id && v.user_id === me.uid)).concat([{ post_id: id, user_id: me.uid, option: opt }]);
    nvKeepDraft(() => {});
  } catch (e) { toast(nvV8err(e) ? V8_MSG : e.message, true, 6000); }
}
async function nvWallLike(id, b) {
  if (!(await revNeed())) return; const me = revMe(), w = (rev.wall || []).find(x => x.id === id); if (!w) return;
  const on = rev.wallLiked.has(id), n = revCnt(w.wall_likes);
  on ? rev.wallLiked.delete(id) : rev.wallLiked.add(id); w.wall_likes = [{ count: Math.max(0, n + (on ? -1 : 1)) }];
  if (b) { b.classList.toggle('on', !on); b.setAttribute('aria-pressed', String(!on)); const s = b.querySelector('span'); if (s) s.textContent = revCnt(w.wall_likes); }
  try {
    if (on) await revFetch(`/rest/v1/wall_likes?post_id=eq.${id}&user_id=eq.${me.uid}`, { method: 'DELETE' });
    else await revFetch('/rest/v1/wall_likes', { method: 'POST', body: { post_id: id, user_id: me.uid }, prefer: 'return=minimal' });
  } catch (e) { on ? rev.wallLiked.add(id) : rev.wallLiked.delete(id); w.wall_likes = [{ count: n }]; nvDrawWall(); toast(e.message, true); }
}
async function nvWallDel(id) {
  const w = (rev.wall || []).find(x => x.id === id); if (!w) return;
  if (!(await uiConfirm('¿Borrar esta nota?', w.parent_id ? 'No se puede deshacer.' : 'Se borran también sus respuestas. No se puede deshacer.', 'Borrar'))) return;
  await revFetch(`/rest/v1/wall_posts?id=eq.${id}`, { method: 'DELETE' }); revUnPair(w.image); toast('Nota borrada.'); return nvWall();
}
async function nvMention(name) {
  try { const a = await revFetch(`/rest/v1/profiles?select=id,name&name=ilike.${encodeURIComponent(revPat(name) + '*')}&limit=5`); const x = a.find(z => revNk(z.name) === revNk(name)) || a[0]; if (x) return revProfile(x.id); toast('No encontré a esa persona.'); } catch (e) { toast(e.message, true); }
}
/* Imagen para historias (notas, conciertos, club) */
async function nvStory(o) {
  try {
    const [c, g] = revCanvas(1080, 1920);
    const gr = g.createLinearGradient(0, 0, 1080, 1920); gr.addColorStop(0, '#2a1f3d'); gr.addColorStop(.6, '#1b1e23'); gr.addColorStop(1, '#3a2418'); g.fillStyle = gr; g.fillRect(0, 0, 1080, 1920);
    const sun = g.createRadialGradient(920, 240, 10, 920, 240, 560); sun.addColorStop(0, 'rgba(255,138,92,.5)'); sun.addColorStop(1, 'rgba(255,95,158,0)'); g.fillStyle = sun; g.fillRect(0, 0, 1080, 1920);
    let y = 170, im = null;
    if (o.img) { try { im = await revImgEl(o.img); } catch {} }
    if (im) { g.save(); g.beginPath(); if (g.roundRect) g.roundRect(70, 170, 940, 700, 40); else g.rect(70, 170, 940, 700); g.clip(); revCoverFit(g, im, 70, 170, 940, 700); g.restore(); y = 930; }
    try { await document.fonts.load('800 80px "Bricolage Grotesque"'); } catch {}
    g.textBaseline = 'top'; g.fillStyle = '#f2c230'; g.font = '700 46px "Barlow Condensed", sans-serif'; g.fillText(String(o.kicker || '').toUpperCase().slice(0, 42), 70, y); y += 84;
    const size = im ? 72 : 88; g.fillStyle = '#fff'; g.font = `800 ${size}px "Bricolage Grotesque", Barlow, sans-serif`;
    const lines = []; let line = ''; String(o.title || '').replace(/\s+/g, ' ').split(' ').forEach(w => { const t = line ? line + ' ' + w : w; if (g.measureText(t).width > 940 && line) { lines.push(line); line = w; } else line = t; }); if (line) lines.push(line);
    const max = im ? 5 : 9; lines.slice(0, max).forEach((l, i) => { g.fillText(i === max - 1 && lines.length > max ? l + '…' : l, 70, y); y += size * 1.18; });
    if (o.sub) { g.font = '500 42px Barlow, sans-serif'; g.fillStyle = '#d6dbe2'; g.fillText(String(o.sub).slice(0, 52), 70, y + 20); }
    g.fillStyle = '#f2c230'; g.fillRect(70, 1770, 120, 10); g.font = '700 40px "Barlow Condensed", sans-serif'; g.fillStyle = '#fff'; g.fillText('RADIO DE RUTA · REVISTA', 70, 1800);
    await revSaveCanvas(c, o.name || 'radio-de-ruta', true);
  } catch (e) { toast('No pude crear la imagen.', true); }
}
/* Imagen de fondo del perfil (en «Editar perfil») */
const nvDrawBan = () => { const el = $('nvBanPrev'); if (!el) return; const src = rev.pban && (rev.pban.startsWith('data:') ? rev.pban : revPic(rev.pban)); el.innerHTML = src ? `<img src="${esc(src)}" alt="">` : `<span class="nv-ban0" style="--h:${nvHue((rev.me || {}).name)}"></span>`; };
async function nvAct(act, b, d) {
  switch (act) {
    case 'nvmore': return nvMore();
    case 'nvclose': return nvCloseSheet();
    case 'nvlegal': return window.open('legal.html', '_blank', 'noopener');
    case 'nvwpost': return nvWallPost(b);
    case 'nvwimg': return $('nvWFile').click();
    case 'nvwnoimg': rev.nvWImg = ''; return nvDrawWImg();
    case 'nvwlike': return nvWallLike(d.id, b);
    case 'nvwpoll': return nvKeepDraft(() => { rev.nvPollOn = !rev.nvPollOn; });
    case 'nvwded': return nvKeepDraft(() => { rev.nvDed = !rev.nvDed; });
    case 'nvwrep': { if (!(await revNeed())) return; const bx = $('nvRC-' + d.id); if (bx) { bx.hidden = !bx.hidden; if (!bx.hidden) bx.querySelector('textarea').focus(); } return; }
    case 'nvwreply': return nvWallReply(d.id, b);
    case 'nvwvote': return nvWallVote(d.id, +d.v);
    case 'nvat': return nvMention(d.v);
    case 'nvwstory': { const w = (rev.wall || []).find(x => x.id === d.id); if (!w) return; return nvStory({ kicker: 'Muro de ' + rev.prof.name, title: w.body, sub: '— ' + ((w.profiles && w.profiles.name) || ''), img: revPic(w.image), name: 'nota-radio-de-ruta' }); }
    case 'nvnowplay': { const n = revNk(d.v), a = revNk(d.a); try { const all = await libAll(); if (all.some(r => revNk(r.name) === n && revNk(r.artist) === a)) return playLib(r => revNk(r.name) === n && revNk(r.artist) === a); if (all.some(r => revNk(r.artist) === a)) { toast('Esa canción no está en tu biblioteca; pongo ese artista.'); return playLib(r => revNk(r.artist) === a); } } catch {} return toast('No está en tu biblioteca.', true); }
    case 'nvwdel': return nvWallDel(d.id);
    case 'nvgocom': { const c = document.querySelector('#revBody .rv-com'); if (c) c.scrollIntoView({ behavior: 'smooth', block: 'start' }); const t = $('rvCom'); if (t) setTimeout(() => t.focus({ preventScroll: true }), 400); return; }
    case 'nvpickban': return $('nvBanFile').click();
    case 'nvnoban': rev.pban = ''; return nvDrawBan();
  }
}
/* «Escuchando ahora» en el perfil (si la persona lo activa en Editar perfil) */
function nvNowHtml(p) {
  const n = p && p.now_playing; if (!n || !n.n || !n.at || Date.now() - n.at > 45 * 60e3) return '';
  const m = Math.max(0, Math.round((Date.now() - n.at) / 60000));
  return `<button type="button" class="nv-now" data-act="nvnowplay" data-v="${esc(n.n)}" data-a="${esc(n.a || '')}"><span class="nv-eq" aria-hidden="true"><i></i><i></i><i></i></span><span class="nv-nowt"><small>Escuchando ahora · ${m < 2 ? 'ahora mismo' : `hace ${m} min`}</small><b>${esc(n.n)}</b><span>${esc(n.a || '')}</span></span>${ICO('play')}</button>`;
}

/* --- Club de escucha: un disco por semana, comentado canción por canción --- */
const CLUB_ACTS = new Set(['clopen', 'clcom', 'cldelc', 'cledit', 'clnew', 'clsave', 'cldel', 'clsum', 'clplay', 'clstory', 'clpickcov', 'clnocov', 'clsec']);
const clubState = c => { const t = revToday10(); return c.starts_on > t ? 'soon' : c.ends_on <= t ? 'past' : 'now'; };
const clubRange = c => `${vaultD(c.starts_on)} al ${vaultD(vaultAdd(c.ends_on, -1))}`;
const clubCover = (c, cls = '') => { const im = revPic(c.cover); return im ? `<img${cls ? ` class="${cls}"` : ''} src="${esc(im)}" alt="" loading="lazy" decoding="async">` : `<span class="cl-noc" style="--h:${nvHue(c.album + c.artist)}">${esc((c.album || '?').slice(0, 1))}</span>`; };
function nvClubBanner(c) {
  if (!c) return '';
  return `<button type="button" class="nv-vb cl-ban" data-act="clopen" data-id="${esc(c.id)}"><span class="nv-vbimg">${clubCover(c)}</span><span class="nv-vbtx"><span class="nv-vbk">${ICO('music')} Club de escucha</span><b>${esc(c.album)}</b><small>${esc([c.artist, c.year].filter(Boolean).join(' · '))} · hasta el ${esc(vaultD(vaultAdd(c.ends_on, -1)))}</small><span class="nv-vbgo">${ICO('chat')} Comentar el disco</span></span></button>`;
}
async function clubView(id, noPush) {
  if (id && !noPush) revPush();
  rev.sec = 'club'; rev.view = 'club'; revBar(); const my = ++rev.req, box = revBody();
  box.innerHTML = '<p class="hint">Abriendo el club…</p>';
  try {
    const list = await revFetch('/rest/v1/clubs?select=*&order=starts_on.desc&limit=40');
    if (my !== rev.req) return;
    rev.clubs = list;
    const c = (id && list.find(x => x.id === id)) || list.find(x => clubState(x) === 'now') || null;
    rev.club = c; rev.clubComs = [];
    if (c) rev.clubComs = await revFetch(`/rest/v1/club_comments?select=*,profiles!author_id(name,avatar)&club_id=eq.${c.id}&order=created_at.asc&limit=600`);
    if (my !== rev.req) return;
    clubDraw();
  } catch (e) { if (my === rev.req) box.innerHTML = `<p class="hint">${esc(/actualizar la base|clubs|schema cache|does not exist/i.test(e.message + (e.raw || '')) ? V8_MSG : e.message)}</p>`; }
}
function clubDraw() {
  const c = rev.club, me = revMe(), ed = revIsEditor(), box = revBody(), list = rev.clubs || [];
  const intro = `<header class="bv-mast cl-mast"><p class="bv-kick">Un disco por semana · comentado canción por canción</p><h2>Club de escucha</h2><p class="bv-sub">Escucha el disco cuando quieras durante la semana y deja lo que sentiste en cada canción. Al final se publica un resumen en la Revista.</p></header>`;
  const other = list.filter(x => !c || x.id !== c.id);
  const chips = other.length ? `<section class="nv-row"><div class="nv-rowh"><h4>Otros discos del club</h4></div><div class="nv-hscroll">${other.map(x => `<button type="button" class="nv-pst cl-pst" data-act="clopen" data-id="${esc(x.id)}">${clubCover(x)}<b>${esc(x.album)}</b><small>${esc(x.artist)} · ${clubState(x) === 'soon' ? 'próximamente' : clubState(x) === 'past' ? 'ya pasó' : 'esta semana'}</small></button>`).join('')}</div></section>` : '';
  const tools = ed ? `<div class="bv-tools"><span class="rv-m">Editor:</span><button type="button" class="alt" data-act="clnew">Programar disco</button>${c ? '<button type="button" class="alt" data-act="cledit">Editar</button><button type="button" class="alt" data-act="clsum">Escribir resumen en la Revista</button><button type="button" class="alt" data-act="cldel">Borrar</button>' : ''}</div>` : '';
  if (!c) { box.innerHTML = intro + `<div class="bv-empty"><b>No hay disco esta semana.</b><p>${ed ? 'Toca «Programar disco» para elegir el primero.' : 'Pronto habrá un disco nuevo para escuchar juntos.'}</p></div>` + tools + chips; return; }
  const tracks = Array.isArray(c.tracks) ? c.tracks : [], st = clubState(c);
  const cm = x => `<div class="rv-c"><p class="rv-m"><a href="#" data-act="profile" data-id="${esc(x.author_id)}">${revAv(x.profiles, 22)} <b>${esc(revName(x))}</b></a> · ${revAgo(x.created_at)}${me && (me.uid === x.author_id || ed) ? ` <button type="button" class="rv-x" data-act="cldelc" data-id="${esc(x.id)}">borrar</button>` : ''}${me && me.uid !== x.author_id ? ` <button type="button" class="rv-x" data-act="report" data-type="ccomment" data-id="${esc(x.id)}">denunciar</button>` : ''}</p><p>${nvRich(x.body)}</p></div>`;
  const thread = (i, open) => { const xs = rev.clubComs.filter(x => x.track === i); return `<div class="cl-th">${xs.map(cm).join('') || `<p class="hint">${i < 0 ? 'Cuenta qué te pareció el disco completo.' : 'Nadie ha comentado esta canción todavía.'}</p>`}${me ? `<textarea id="clT${i}" rows="2" maxlength="1000" placeholder="${i < 0 ? 'Tu opinión del disco…' : 'Qué te hizo sentir esta canción…'}"></textarea><button type="button" data-act="clcom" data-v="${i}">Comentar</button>` : '<p class="hint">Entra a tu cuenta para comentar.</p>'}</div>`; };
  const n = i => rev.clubComs.filter(x => x.track === i).length;
  box.innerHTML = intro + `<article class="cl-card"><div class="cl-head">${clubCover(c, 'cl-cov')}<div><p class="cl-k">${st === 'now' ? 'Disco de la semana' : st === 'soon' ? 'Próximamente' : 'Ya pasó'} · ${esc(clubRange(c))}</p><h3>${esc(c.album)}</h3><p class="cl-a">${esc([c.artist, c.year].filter(Boolean).join(' · '))}</p></div></div>
${c.note ? `<div class="rv-txt cl-note">${revFmt(c.note)}</div>` : ''}<div class="rv-pbtns"><button type="button" class="rv-like" data-act="clplay" id="clPlay" hidden>${ICO('play')} Escuchar en mi biblioteca</button><button type="button" class="rv-like" data-act="clstory">${ICO('image')} Imagen para historias</button></div></article>
${tools}
<h4 class="rv-h">Canción por canción</h4><div class="cl-tracks">${tracks.map((t, i) => `<details class="cl-tr"${rev.clubOpen === i ? ' open' : ''} data-i="${i}"><summary><span class="cl-n">${i + 1}</span><span class="cl-tn">${esc(t)}</span><span class="cl-cc">${ICO('chat')} ${n(i)}</span></summary>${thread(i)}</details>`).join('') || '<p class="hint">El editor todavía no agregó la lista de canciones.</p>'}</div>
<h4 class="rv-h">El disco completo (${n(-1)})</h4>${thread(-1)}` + chips;
  box.querySelectorAll('.cl-tr').forEach(d => d.addEventListener('toggle', () => { if (d.open) rev.clubOpen = +d.dataset.i; }));
  if (typeof libAll === 'function') libAll().then(all => { const a = revNk(c.artist), al = revNk(c.album); if (rev.club === c && all.some(r => revNk(r.artist) === a)) { const b = $('clPlay'); if (b) b.hidden = false; } }).catch(() => {});
}
function clubEdit(c) {
  if (!revIsEditor()) return toast('Solo el admin y los editores programan el club.', true);
  revPush(); ++rev.req; rev.view = 'editclub'; rev.clEdit = c || null; rev.clCov = c ? c.cover || '' : ''; revBar();
  const x = c || {}, next = (() => { const used = new Set((rev.clubs || []).filter(z => !c || z.id !== c.id).map(z => z.starts_on)); let d = revToday10(); const busy = (rev.clubs || []).filter(z => (!c || z.id !== c.id) && clubState(z) !== 'past').map(z => z.ends_on).sort().pop(); if (busy && busy > d) d = busy; while (used.has(d)) d = vaultAdd(d, 1); return d; })();
  revBody().innerHTML = revHead('Cancelar', c ? 'Editar disco del club' : 'Programar disco del club') + `<div class="rv-form">
<label for="clAl">Disco</label><input id="clAl" maxlength="120" value="${esc(x.album || '')}" placeholder="Ej: Canción Animal">
<label for="clAr">Artista</label><input id="clAr" maxlength="100" value="${esc(x.artist || '')}">
<label for="clY">Año</label><input id="clY" inputmode="numeric" maxlength="4" value="${esc(x.year || '')}">
<label for="clTr">Canciones (una por línea, en orden)</label><textarea id="clTr" rows="8" placeholder="(En) el séptimo día&#10;Un millón de años luz&#10;…">${esc((x.tracks || []).join('\n'))}</textarea>
<label for="clNo">Por qué este disco (opcional)</label><textarea id="clNo" rows="4" maxlength="2000">${esc(x.note || '')}</textarea>
<div class="bv-row"><div><label for="clD">Empieza</label><input id="clD" type="date" value="${esc(x.starts_on || next)}"></div><div><label for="clDays">Días</label><input id="clDays" type="number" min="1" max="60" value="${esc(x.days || 7)}"></div></div>
<label>Portada (opcional; usa una foto tuya o una imagen libre)</label><div id="clCovP" class="bv-cov"></div><div class="rv-pcb"><button type="button" class="alt" data-act="clpickcov">Elegir imagen</button><button type="button" class="alt" data-act="clnocov">Quitar</button></div><input type="file" id="clCovF" accept="image/*" hidden>
<div class="row" style="margin-top:16px"><button type="button" data-act="clsave">${c ? 'Guardar cambios' : 'Programar'}</button></div></div>`;
  clubDrawCov();
  $('clCovF').onchange = async e => { const f = e.target.files && e.target.files[0]; e.target.value = ''; if (!f) return; try { rev.clCov = (await revResize(f, 1000, 0.85, true)).data(); clubDrawCov(); } catch { toast('No se pudo leer esa imagen.', true); } };
}
const clubDrawCov = () => { const el = $('clCovP'); if (el) { const s = rev.clCov && (rev.clCov.startsWith('data:') ? rev.clCov : revPic(rev.clCov)); el.innerHTML = s ? `<img src="${esc(s)}" alt="">` : '<span class="rv-m">Sin portada</span>'; } };
async function clubSave(btn) {
  const v = id => ($(id).value || '').trim(), album = v('clAl'), artist = v('clAr'), y = v('clY');
  if (!album || !artist) return toast('Escribe el disco y el artista.', true);
  if (y && !/^(19|20)\d{2}$/.test(y)) return toast('El año debe tener 4 cifras.', true);
  const tracks = $('clTr').value.split('\n').map(t => t.trim().replace(/^\d+[.)-]?\s*/, '').slice(0, 100)).filter(Boolean).slice(0, 40);
  const days = Math.round(+v('clDays') || 7); if (days < 1 || days > 60) return toast('Los días van de 1 a 60.', true);
  const body = { album: album.slice(0, 120), artist: artist.slice(0, 100), year: y ? +y : null, tracks, note: v('clNo').slice(0, 2000) || null, starts_on: v('clD') || revToday10(), days };
  if (btn) btn.disabled = true;
  try {
    body.cover = rev.clCov && rev.clCov.startsWith('data:') ? await revUploadPair(rev.clCov, 'club-' + revUid(), 1000, 360) : (revPic(rev.clCov) || null);
    let id;
    if (rev.clEdit) { await revFetch(`/rest/v1/clubs?id=eq.${rev.clEdit.id}`, { method: 'PATCH', body, prefer: 'return=minimal' }); id = rev.clEdit.id; if (rev.clEdit.cover && rev.clEdit.cover !== body.cover) revUnPair(rev.clEdit.cover); }
    else { const r = await revFetch('/rest/v1/clubs', { method: 'POST', body: { ...body, created_by: revMe().uid }, prefer: 'return=representation' }); id = r && r[0] && r[0].id; }
    toast('Disco del club guardado.'); rev.stack.pop(); return clubView(id, true);
  } catch (e) { if (btn) btn.disabled = false; toast(/clubs|schema cache/i.test(e.message + (e.raw || '')) ? V8_MSG : e.message, true, 6000); }
}
function clubSummary() {
  const c = rev.club; if (!c) return;
  const tracks = Array.isArray(c.tracks) ? c.tracks : [], q = x => `> «${x.body.replace(/\s+/g, ' ').slice(0, 280)}» — ${revName(x)}`;
  const parts = [`Durante ${c.days} días escuchamos juntos **${c.album}** de ${c.artist}${c.year ? ` (${c.year})` : ''}. Esto fue lo que dejó la comunidad en el club de escucha.`];
  const gen = rev.clubComs.filter(x => x.track === -1).slice(0, 3); if (gen.length) parts.push('## El disco completo', gen.map(q).join('\n\n'));
  tracks.forEach((t, i) => { const xs = rev.clubComs.filter(x => x.track === i).slice(0, 2); if (xs.length) parts.push(`## ${i + 1}. ${t}`, xs.map(q).join('\n\n')); });
  if (parts.length === 1) parts.push('Todavía no hay comentarios: completa este resumen con lo que sentiste al escucharlo.');
  try { localStorage.setItem('rr_rev_draft', JSON.stringify({ title: `Club de escucha: ${c.album}`.slice(0, 150), subtitle: `Lo que sentimos al escuchar a ${c.artist}, canción por canción`.slice(0, 200), body: parts.join('\n\n').slice(0, 19000), tags: ['club de escucha'], tracks: [] })); } catch {}
  rev.sec = 'art'; revEditor(null);
  setTimeout(() => { if ($('rvTitle') && !$('rvTitle').value) $('rvTitle').value = `Club de escucha: ${c.album}`; if ($('rvText') && !$('rvText').value) $('rvText').value = parts.join('\n\n'); }, 50);
}
async function clubAct(act, b, d) {
  const c = rev.club;
  switch (act) {
    case 'clsec': return clubView();
    case 'clopen': return clubView(d.id, rev.view === 'club' ? true : false);
    case 'clcom': {
      if (!(await revNeed())) return; const i = +d.v, ta = $('clT' + i), t = ((ta && ta.value) || '').trim(); if (!t) return;
      try { await revFetch('/rest/v1/club_comments', { method: 'POST', body: { club_id: c.id, track: i, author_id: revMe().uid, body: t.slice(0, 1000) }, prefer: 'return=minimal' }); }
      catch (e) { return toast(e.message, true, 6000); }
      if (i >= 0) rev.clubOpen = i; rev.clubComs = await revFetch(`/rest/v1/club_comments?select=*,profiles!author_id(name,avatar)&club_id=eq.${c.id}&order=created_at.asc&limit=600`); return clubDraw();
    }
    case 'cldelc': if (!(await uiConfirm('¿Borrar comentario?', 'No se puede deshacer.', 'Borrar'))) return; await revFetch(`/rest/v1/club_comments?id=eq.${d.id}`, { method: 'DELETE' }); rev.clubComs = rev.clubComs.filter(x => x.id !== d.id); return clubDraw();
    case 'clnew': return clubEdit(null);
    case 'cledit': return clubEdit(c);
    case 'clsave': return clubSave(b);
    case 'cldel': if (!(await uiConfirm('¿Borrar este disco del club?', 'Se borran también sus comentarios.', 'Borrar'))) return; await revFetch(`/rest/v1/clubs?id=eq.${c.id}`, { method: 'DELETE' }); revUnPair(c.cover); toast('Disco borrado.'); return clubView(null, true);
    case 'clsum': return clubSummary();
    case 'clplay': { const a = revNk(c.artist), al = revNk(c.album); const all = await libAll(); const hasAl = all.some(r => revNk(r.artist) === a && revNk(r.album) === al); return playLib(r => revNk(r.artist) === a && (!hasAl || revNk(r.album) === al)); }
    case 'clstory': return nvStory({ kicker: 'Club de escucha', title: c.album, sub: [c.artist, c.year].filter(Boolean).join(' · '), img: revPic(c.cover), name: 'club-radio-de-ruta' });
    case 'clpickcov': return $('clCovF').click();
    case 'clnocov': rev.clCov = ''; return clubDrawCov();
  }
}

/* --- Guardados sin conexión y listas compartidas --- */
const EX_ACTS = new Set(['svsave', 'svlist', 'svopen', 'svdel', 'lsindex', 'lstab', 'lsopen', 'lsnew', 'lsedit', 'lssave', 'lsdel', 'lsadd', 'lsrm', 'lsplay', 'lsplayone', 'lsopenmode', 'lslink']);
const V9_MSG = 'Falta activar las listas compartidas: ejecuta otra vez revista_supabase_completo.sql en Supabase (SQL Editor). No borra nada.';
/* Guardados: los artículos quedan en este celular para leerlos sin internet */
let svDbP = null;
const svDb = () => svDbP || (svDbP = new Promise((ok, no) => { const r = indexedDB.open('rr-saved', 1); r.onupgradeneeded = () => r.result.createObjectStore('a', { keyPath: 'id' }); r.onsuccess = () => ok(r.result); r.onerror = () => { svDbP = null; no(r.error); }; }));
const svTx = async (mode, f) => { const d = await svDb(); return new Promise((ok, no) => { const q = f(d.transaction('a', mode).objectStore('a')); q.onsuccess = () => ok(q.result); q.onerror = () => no(q.error); }); };
const svAll = () => svTx('readonly', s => s.getAll()).catch(() => []);
const svHas = id => svTx('readonly', s => s.count(id)).then(n => n > 0).catch(() => false);
async function svPic(u, w = 1200) {
  const src = revPic(u); if (!src) return '';
  if (revImg(src)) return src;
  try {
    const r = await withTimeout(fetch(src, { mode: 'cors' }), 15000); if (!r.ok) return '';
    const bm = await createImageBitmap(await r.blob()), k = Math.min(1, w / bm.width), c = document.createElement('canvas');
    c.width = Math.round(bm.width * k); c.height = Math.round(bm.height * k); c.getContext('2d').drawImage(bm, 0, 0, c.width, c.height);
    const d = c.toDataURL('image/jpeg', .82); return revImg(d) ? d : '';
  } catch { return ''; }
}
async function svSave() {
  const p = rev.post; if (!p) return;
  if (await svHas(p.id)) { await svTx('readwrite', s => s.delete(p.id)); svBtn(false); return toast('Quitado de Guardados.'); }
  const b = $('svBtn'); if (b) { b.disabled = true; b.innerHTML = ICO('download') + ' Guardando…'; }
  try {
    const cover = await svPic(p.cover), gallery = [];
    for (const g of (Array.isArray(p.gallery) ? p.gallery : []).slice(0, 8)) { const d = await svPic(typeof g === 'string' ? g : g && g.src, 900); if (d) gallery.push(typeof g === 'string' ? d : { ...g, src: d }); }
    const rec = { id: p.id, title: p.title, subtitle: p.subtitle || '', body: p.body || '', style: p.style || null, review: p.review || null, tracks: Array.isArray(p.tracks) ? p.tracks.slice(0, 12) : [], cover, gallery, author: revName(p), author_id: p.author_id, created_at: p.created_at, savedAt: Date.now() };
    await svTx('readwrite', s => s.put(rec)); svBtn(true); toast('Guardado: lo puedes leer sin internet en Más › Guardados.');
  } catch (e) { svBtn(false); toast('No se pudo guardar: ' + (e.message || 'sin espacio'), true); }
  finally { if (b) b.disabled = false; }
}
function svBtn(on) { const b = $('svBtn'); if (b) { b.classList.toggle('on', on); b.setAttribute('aria-pressed', String(on)); b.innerHTML = ICO('download') + (on ? ' Guardado sin conexión' : ' Guardar sin conexión'); } }
async function svList(noPush) {
  if (!noPush) revPush();
  rev.sec = 'more'; rev.view = 'saved'; ++rev.req; revBar();
  const a = (await svAll()).sort((x, y) => y.savedAt - x.savedAt);
  revBody().innerHTML = revHead('Volver', 'Guardados') + `<p class="hint">${a.length ? 'Estos artículos están en tu celular: se leen aunque no tengas internet.' : 'Aún no guardas artículos. Abre uno y toca «Guardar sin conexión» para leerlo en el camino, aunque no haya señal.'}</p>
<div class="sv-list">${a.map(r => `<div class="sv-row rv-click" data-act="svopen" data-id="${esc(r.id)}" tabindex="0">${r.cover ? `<img src="${esc(r.cover)}" alt="">` : `<span class="sv-ph" style="--h:${nvHue(r.title)}">${ICO('news')}</span>`}<div><b>${esc(r.title)}</b><small>${esc(r.author)} · guardado ${revAgo(new Date(r.savedAt).toISOString())}</small></div><button type="button" class="rv-x" data-act="svdel" data-id="${esc(r.id)}" aria-label="Quitar de Guardados">Quitar</button></div>`).join('')}</div>`;
}
async function svOpen(id, noPush) {
  const r = await svTx('readonly', s => s.get(id)).catch(() => null); if (!r) return svList(true);
  if (!noPush) revPush();
  rev.sec = 'more'; rev.view = 'svitem'; rev.svId = id; ++rev.req; revBar();
  const st = revSt(r.style), rv = revReview(r.review);
  const lib = await libAll().catch(() => []), map = new Map(); lib.forEach(x => map.set(revNk(x.name) + '|' + revNk(x.artist), x.id));
  rev.ids = []; const tr = r.tracks.map(t => { const lid = map.get(revNk(t.name) + '|' + revNk(t.artist)); if (lid !== undefined) rev.ids.push(lid); return `<div class="rv-tr">${ICO('music')}<div><b>${esc(t.name || '')}</b><span>${esc(t.artist || '')}</span></div>${lid !== undefined ? `<button type="button" class="alt" data-act="play" data-i="${rev.ids.length - 1}">Reproducir</button>` : '<em>No está en tu biblioteca</em>'}</div>`; }).join('');
  const by = `<div class="rv-by nv-by"><span><b>${esc(r.author)}</b><small>${revDate(r.created_at)} · guardado en este celular</small></span></div>`;
  revBody().innerHTML = revHead('Volver', '', `<span class="rv-sp"></span>${navigator.onLine ? `<button type="button" class="alt" data-act="open" data-id="${esc(r.id)}">Ver en línea</button>` : ''}`) +
    `<article class="rv-art rv-st" ${revAttrs(st)}>${revHeader(r.title, r.subtitle, r.cover, st, by, r.created_at)}${rv ? revReviewBox(rv) : ''}
<div class="rv-pbtns rv-listen"><button type="button" class="rv-like" data-act="read">${ICO(rev.reading ? 'stop' : 'mic')} ${rev.reading ? 'Detener lectura' : 'Escuchar artículo'}</button>${rev.ids.length ? `<button type="button" class="rv-like" data-act="playlist">${ICO('play')} Escuchar esta lista (${rev.ids.length})</button>` : ''}</div>
<div class="rv-txt" data-dc="${st.dc}">${revFmt(r.body)}</div>${revGallery(r.gallery)}${tr ? `<h4 class="rv-h">Canciones del artículo</h4>${tr}` : ''}</article>`;
  rev.post = { id: r.id, title: r.title, subtitle: r.subtitle, body: r.body, author_id: r.author_id, profiles: { name: r.author }, saved: true };
}
function svOfflineHtml() { return '<div class="bv-empty"><b>Sin internet</b><p>La revista necesita conexión, pero tus artículos guardados se leen sin señal.</p><button type="button" data-act="svlist">Ver mis Guardados</button></div>'; }
/* Listas compartidas */
const lsCan = l => { const me = revMe(); return !!me && !revBanned() && (l.owner_id === me.uid || l.open); };
async function lsIndex(noPush) {
  if (!noPush) revPush();
  rev.sec = 'more'; rev.view = 'lists'; rev.lsTab = rev.lsTab || 'all'; revBar(); const my = ++rev.req, box = revBody(), me = revMe();
  box.innerHTML = '<p class="hint">Abriendo las listas…</p>';
  try {
    const who = rev.lsTab === 'mine' && me ? `&owner_id=eq.${me.uid}` : '';
    const a = await revFetch(`/rest/v1/song_lists?select=*,profiles!owner_id(name,avatar),song_list_items(count)${who}&order=created_at.desc&limit=40`);
    if (my !== rev.req) return;
    box.innerHTML = `<header class="bv-mast ls-mast"><p class="bv-kick">Armadas por la comunidad</p><h2>Listas</h2><p class="bv-sub">Listas de canciones para el camino, para una fiesta o para alguien. Una lista abierta deja que cualquiera con cuenta agregue canciones.</p></header>
<div class="rv-tabs nv-tabs"><button type="button" class="rv-tab${rev.lsTab === 'all' ? ' on' : ''}" data-act="lstab" data-v="all">Recientes</button>${me ? `<button type="button" class="rv-tab${rev.lsTab === 'mine' ? ' on' : ''}" data-act="lstab" data-v="mine">Mis listas</button>` : ''}<span class="rv-sp"></span>${me && !revBanned() ? `<button type="button" data-act="lsnew">${ICO('plus')} Nueva lista</button>` : ''}</div>
<div class="ls-grid">${a.map(lsCard).join('') || `<p class="hint">${rev.lsTab === 'mine' ? 'Aún no tienes listas. Crea la primera.' : 'Aún no hay listas. ¡Crea la primera!'}</p>`}</div>`;
  } catch (e) { if (my === rev.req) box.innerHTML = revHead('Volver', 'Listas') + `<p class="hint">${esc(/song_lists|schema cache|does not exist/i.test(e.message + (e.raw || '')) ? V9_MSG : e.message)}</p>`; }
}
const lsCard = l => `<button type="button" class="ls-card" data-act="lsopen" data-id="${esc(l.id)}" style="--h:${nvHue(l.title)}"><span class="ls-art">${ICO('music')}</span><b>${esc(l.title)}</b><small>${revCnt(l.song_list_items)} canciones · ${esc(revName({ profiles: l.profiles }))}${l.open ? ' · abierta' : ''}</small></button>`;
async function lsOpen(id, noPush) {
  if (!UUID.test(id)) return;
  if (!noPush) revPush();
  rev.sec = 'more'; rev.view = 'list'; rev.lsId = id; revBar(); const my = ++rev.req, box = revBody();
  box.innerHTML = '<p class="hint">Cargando la lista…</p>';
  try {
    const [l, it] = await Promise.all([revFetch(`/rest/v1/song_lists?select=*,profiles!owner_id(name,avatar)&id=eq.${id}`), revFetch(`/rest/v1/song_list_items?select=*,profiles!added_by(name)&list_id=eq.${id}&order=created_at.asc&limit=200`)]);
    if (my !== rev.req) return;
    if (!l[0]) { box.innerHTML = revHead('Volver', '') + '<p class="hint">Esa lista ya no existe.</p>'; return; }
    rev.ls = l[0]; rev.lsItems = it; await lsDraw();
  } catch (e) { if (my === rev.req) box.innerHTML = revHead('Volver', '') + `<p class="hint">${esc(/song_lists|schema cache|does not exist/i.test(e.message + (e.raw || '')) ? V9_MSG : e.message)}</p>`; }
}
async function lsDraw() {
  const l = rev.ls, me = revMe(), mine = me && me.uid === l.owner_id, adm = revIsAdmin();
  const lib = await libAll().catch(() => []), map = new Map(); lib.forEach(r => { const k = revNk(r.name) + '|' + revNk(r.artist); if (!map.has(k)) map.set(k, r.id); });
  rev.lsIds = [];
  const rows = rev.lsItems.map((x, i) => {
    const lid = map.get(revNk(x.title) + '|' + revNk(x.artist)); if (lid !== undefined) rev.lsIds.push(lid);
    const rm = me && (me.uid === x.added_by || mine || adm);
    return `<div class="rv-tr ls-tr"><span class="cl-n">${i + 1}</span><div><b>${esc(x.title)}</b><span>${esc(x.artist)}${x.added_by !== l.owner_id ? ` · la agregó ${esc(revName(x))}` : ''}</span></div>${lid !== undefined ? `<button type="button" class="alt" data-act="lsplayone" data-v="${esc(String(lid))}">Reproducir</button>` : x.link ? `<a class="alt ls-lk" href="${esc(x.link)}" target="_blank" rel="noopener noreferrer">Abrir enlace</a>` : '<em>No está en tu biblioteca</em>'}${rm ? `<button type="button" class="rv-x" data-act="lsrm" data-id="${esc(x.id)}" aria-label="Quitar «${esc(x.title)}»">Quitar</button>` : ''}</div>`;
  }).join('');
  const add = lsCan(l) && rev.lsItems.length < 200 ? `<div class="rv-form ls-add"><h4 class="rv-h">Agregar una canción</h4><div class="bv-row"><div><label for="lsT">Canción</label><input id="lsT" maxlength="120" placeholder="Ej: De música ligera"></div><div><label for="lsA">Artista</label><input id="lsA" maxlength="100" placeholder="Ej: Soda Stereo"></div></div>
<label for="lsL">Enlace (opcional)</label><input id="lsL" maxlength="300" inputmode="url" placeholder="Spotify, YouTube, Bandcamp o SoundCloud"><button type="button" data-act="lsadd">${ICO('plus')} Agregar</button></div>` : !me ? '<p class="hint">Entra con tu cuenta para agregar canciones a las listas abiertas.</p>' : '';
  revBody().innerHTML = revHead('Volver', '', `<span class="rv-sp"></span>${mine ? '<button type="button" class="alt" data-act="lsedit">Editar</button><button type="button" class="alt" data-act="lsdel">Eliminar</button>' : ''}`) +
    `<article class="ls-head" style="--h:${nvHue(l.title)}"><span class="ls-art ls-big">${ICO('music')}</span><div><p class="cl-k">${l.open ? 'Lista abierta · cualquiera con cuenta puede agregar' : 'Lista de ' + esc(revName({ profiles: l.profiles }))}</p><h3>${esc(l.title)}</h3><p class="rv-m"><a href="#" data-act="profile" data-id="${esc(l.owner_id)}">${revAv(l.profiles, 22)} ${esc(revName({ profiles: l.profiles }))}</a> · ${rev.lsItems.length} canciones</p></div></article>
${l.note ? `<div class="rv-txt cl-note">${revFmt(l.note)}</div>` : ''}<div class="rv-pbtns">${rev.lsIds.length ? `<button type="button" class="rv-like" data-act="lsplay">${ICO('play')} Escuchar las que tengo (${rev.lsIds.length})</button>` : ''}${mine ? `<button type="button" class="rv-like" data-act="lsopenmode">${l.open ? 'Cerrar la lista' : 'Abrir la lista a todos'}</button>` : ''}${me && !mine ? `<button type="button" class="rv-x" data-act="report" data-type="list" data-id="${esc(l.id)}">Denunciar</button>` : ''}</div>
<div class="ls-items">${rows || '<p class="hint">La lista está vacía.</p>'}</div>${add}`;
}
function lsEdit(l) {
  if (!(revMe()) || revBanned()) return;
  revPush(); ++rev.req; rev.view = 'editlist'; rev.lsEd = l || null; revBar();
  const x = l || {};
  revBody().innerHTML = revHead('Cancelar', l ? 'Editar lista' : 'Nueva lista') + `<div class="rv-form">
<label for="lsTi">Nombre</label><input id="lsTi" maxlength="80" value="${esc(x.title || '')}" placeholder="Ej: Para el viaje a la playa">
<label for="lsNo">Descripción (opcional)</label><textarea id="lsNo" rows="3" maxlength="500">${esc(x.note || '')}</textarea>
<label class="chk"><input type="checkbox" id="lsOp"${x.open ? ' checked' : ''}> Lista abierta: cualquiera con cuenta puede agregar canciones</label>
<button type="button" data-act="lssave">${l ? 'Guardar cambios' : 'Crear lista'}</button></div>`;
  $('lsTi').focus();
}
async function lsSave() {
  const t = $('lsTi').value.trim(), note = $('lsNo').value.trim(), open = $('lsOp').checked, l = rev.lsEd;
  if (t.length < 2) return toast('Ponle un nombre de al menos 2 letras.', true);
  try {
    if (l) { await revFetch(`/rest/v1/song_lists?id=eq.${l.id}`, { method: 'PATCH', body: { title: t, note: note || null, open }, prefer: 'return=minimal' }); rev.stack.pop(); return lsOpen(l.id, true); }
    const r = await revFetch('/rest/v1/song_lists', { method: 'POST', body: { owner_id: revMe().uid, title: t, note: note || null, open }, prefer: 'return=representation' });
    rev.stack.pop(); toast('Lista creada. Ahora agrega canciones.'); return lsOpen(r[0].id);
  } catch (e) { toast(/song_lists|schema cache|does not exist/i.test(e.message + (e.raw || '')) ? V9_MSG : e.message, true, 6000); }
}
async function lsProfTab(id, my) {
  const a = await revFetch(`/rest/v1/song_lists?select=*,profiles!owner_id(name,avatar),song_list_items(count)&owner_id=eq.${id}&order=created_at.desc&limit=40`);
  if (my !== rev.req) return null;
  const me = revMe();
  return `<div class="ls-grid">${a.map(lsCard).join('') || `<p class="hint">${me && me.uid === id ? 'Aún no tienes listas.' : 'Aún no tiene listas.'}</p>`}</div>${me && me.uid === id && !revBanned() ? `<button type="button" class="alt" data-act="lsnew" style="margin-top:10px">${ICO('plus')} Nueva lista</button>` : ''}`;
}
async function exAct(act, b, d) {
  switch (act) {
    case 'svsave': return svSave();
    case 'svlist': nvCloseSheet(); return svList();
    case 'svopen': return svOpen(d.id);
    case 'svdel': await svTx('readwrite', s => s.delete(d.id)); toast('Quitado de Guardados.'); return svList(true);
    case 'lsindex': nvCloseSheet(); rev.lsTab = 'all'; return lsIndex();
    case 'lstab': rev.lsTab = d.v; return lsIndex(true);
    case 'lsopen': return lsOpen(d.id);
    case 'lsnew': if (!(await revNeed())) return; return lsEdit(null);
    case 'lsedit': return lsEdit(rev.ls);
    case 'lssave': return lsSave();
    case 'lsdel': if (!(await uiConfirm('¿Eliminar esta lista?', 'Se borran también sus canciones. No se puede deshacer.', 'Eliminar'))) return; await revFetch(`/rest/v1/song_lists?id=eq.${rev.ls.id}`, { method: 'DELETE' }); toast('Lista eliminada.'); rev.stack = []; return lsIndex(true);
    case 'lsopenmode': { const v = !rev.ls.open; await revFetch(`/rest/v1/song_lists?id=eq.${rev.ls.id}`, { method: 'PATCH', body: { open: v }, prefer: 'return=minimal' }); rev.ls.open = v; toast(v ? 'Ahora cualquiera con cuenta puede agregar canciones.' : 'Solo tú puedes agregar canciones.'); return lsDraw(); }
    case 'lsadd': {
      if (!(await revNeed())) return;
      const t = $('lsT').value.trim(), a = $('lsA').value.trim(), lk = $('lsL').value.trim();
      if (!t || !a) return toast('Escribe la canción y el artista.', true);
      if (lk && !/^https:\/\/(open\.spotify\.com|(www\.|music\.)?youtube\.com|youtu\.be|[a-z0-9-]+\.bandcamp\.com|soundcloud\.com)\//.test(lk)) return toast('El enlace debe ser de Spotify, YouTube, Bandcamp o SoundCloud (con https://).', true, 6000);
      try { await revFetch('/rest/v1/song_list_items', { method: 'POST', body: { list_id: rev.ls.id, added_by: revMe().uid, title: t.slice(0, 120), artist: a.slice(0, 100), link: lk || null }, prefer: 'return=minimal' }); }
      catch (e) { return toast(/row-level security/i.test(e.message) ? 'Esta lista está cerrada o llegó a 200 canciones.' : e.message, true, 6000); }
      rev.lsItems = await revFetch(`/rest/v1/song_list_items?select=*,profiles!added_by(name)&list_id=eq.${rev.ls.id}&order=created_at.asc&limit=200`); await lsDraw();
      const f = $('lsT'); if (f) f.focus(); return;
    }
    case 'lsrm': await revFetch(`/rest/v1/song_list_items?id=eq.${d.id}`, { method: 'DELETE' }); rev.lsItems = rev.lsItems.filter(x => x.id !== d.id); return lsDraw();
    case 'lsplay': { const ids = new Set(rev.lsIds); return playLib(r => ids.has(r.id), rev.lsIds[0]); }
    case 'lsplayone': { const id = isNaN(+d.v) ? d.v : +d.v; return playLib(r => r.id === id, id); }
  }
}
/* Volver atrás a estas vistas */
{
  const push0 = revPush, back0 = revBack;
  revPush = function () { const v = rev.view; push0(); const s = rev.stack[rev.stack.length - 1]; if (s && s.view === v) { if (v === 'list') s.id = rev.lsId; if (v === 'svitem') s.id = rev.svId; } };
  revBack = function () {
    const s = rev.stack[rev.stack.length - 1];
    if (s && ['lists', 'list', 'saved', 'svitem'].includes(s.view)) { rev.stack.pop(); rev.sec = s.sec; return s.view === 'lists' ? lsIndex(true) : s.view === 'list' ? lsOpen(s.id, true) : s.view === 'saved' ? svList(true) : svOpen(s.id, true); }
    return back0();
  };
  const draw0 = revDrawPost;
  revDrawPost = async function () {
    if (rev.post && rev.post.saved) return rev.view === 'svitem' ? svOpen(rev.svId, true) : undefined;
    await draw0(); const p = rev.post, row = document.querySelector('#revBody .rv-listen');
    if (!p || !row || $('svBtn')) return;
    row.insertAdjacentHTML('beforeend', `<button type="button" class="rv-like" id="svBtn" data-act="svsave" aria-pressed="false">${ICO('download')} Guardar sin conexión</button>`);
    if (await svHas(p.id)) svBtn(true);
  };
}

/* --- Acciones (un solo manejador para toda la vista) --- */
async function revAct(b) {
  const act = b.dataset.act, p = rev.post, d = b.dataset;
  try {
    { const sh = $('nvSheet'); if (sh && sh.contains(b) && act !== 'nvmore') sh.remove(); }
    if (NV_ACTS.has(act)) return await nvAct(act, b, d);
    if (CLUB_ACTS.has(act)) return await clubAct(act, b, d);
    if (EX_ACTS.has(act)) return await exAct(act, b, d);
    if (VAULT_ACTS.has(act)) return await revVaultAct(act, b, d);
    switch (act) {
      case 'sec': return revSection(d.v);
      case 'tab': rev.sec = 'art'; rev.view = 'feed'; rev.tab = d.v; rev.stack = []; revBar(); return revFeed(true);
      case 'retry': return rev.view === 'post' && p ? revOpen(p.id, true) : rev.view === 'artists' ? revArtists() : rev.view === 'posters' ? revPLoad(false) : revFeed(true);
      case 'more': return revFeed(false);
      case 'open': return revOpen(d.id);
      case 'back': return revBack();
      case 'acct': return revAcct();
      case 'logout': return revLogout();
      case 'fab': if (rev.sec === 'vault') return revVaultEdit(null); if (rev.sec === 'art' && rev.tab === 'editions') return revEditEdition(null); if (rev.sec === 'agenda') return revNewEvent(); if (rev.sec === 'art' && rev.tab === 'polls') return revNewPoll(); if (rev.sec === 'posters' && rev.pFilter === 'retos' && revIsAdmin()) return revNewChallenge(); if (rev.sec === 'posters') return revPosterMenu(); if (rev.sec === 'artists') return revEditArtist(null); if (await revNeed()) revEditor(null); return;
      case 'edit': return revEditor(p);
      case 'profile': return revProfile(d.id);
      case 'ptab': return revProfTab(d.v);
      case 'editprof': return revEditProfile();
      case 'saveprof': return revSaveProfile();
      case 'pickav': return $('rvAvFile').click();
      case 'noav': rev.pav = ''; return revStChanged(true);
      case 'shareprof': return revShare(rev.prof.name + ' · Revista', '');
      case 'role': {
        const v = d.v === 'editor' ? 'editor' : 'user';
        if (!(await uiConfirm(v === 'editor' ? '¿Dar permiso de editor?' : '¿Quitar permiso de editor?', v === 'editor' ? 'Podrá crear y editar perfiles de artistas.' : 'Ya no podrá editar perfiles de artistas.', 'Confirmar', false))) return;
        await revFetch(`/rest/v1/profiles?id=eq.${rev.prof.id}`, { method: 'PATCH', body: { role: v }, prefer: 'return=minimal' });
        rev.prof.role = v; toast(v === 'editor' ? 'Ahora es editor.' : 'Permiso quitado.'); return revDrawProfile(), revProfTab(rev.profTab);
      }
      case 'follow': {
        if (!(await revNeed())) return; const id = d.id, me = revMe(), on = rev.follows.has(id);
        if (on) await revFetch(`/rest/v1/follows?follower_id=eq.${me.uid}&author_id=eq.${id}`, { method: 'DELETE' });
        else await revFetch('/rest/v1/follows', { method: 'POST', body: { follower_id: me.uid, author_id: id }, prefer: 'return=minimal' });
        on ? rev.follows.delete(id) : rev.follows.add(id);
        if (rev.view === 'post') return revDrawPost();
        if (rev.view === 'profile') { rev.prof.nFol += on ? -1 : 1; revDrawProfile(); return revProfTab(rev.profTab); }
        return revDrawFeed();
      }
      case 'like': {
        if (!(await revNeed())) return; const me = revMe(), on = rev.likes.has(p.id), n = revCnt(p.likes);
        if (on) await revFetch(`/rest/v1/likes?post_id=eq.${p.id}&user_id=eq.${me.uid}`, { method: 'DELETE' });
        else await revFetch('/rest/v1/likes', { method: 'POST', body: { post_id: p.id, user_id: me.uid }, prefer: 'return=minimal' });
        on ? rev.likes.delete(p.id) : rev.likes.add(p.id); p.likes = [{ count: Math.max(0, n + (on ? -1 : 1)) }]; return revDrawPost();
      }
      case 'sharepost': return revShare(p.title, '#rv=' + p.id);
      case 'comment': {
        if (!(await revNeed())) return; const t = $('rvCom').value.trim(); if (!t) return;
        const body = { post_id: p.id, author_id: revMe().uid, body: t.slice(0, 1000) }; if (rev.replyTo && !rev.v3) body.parent_id = rev.replyTo.id;
        await revFetch('/rest/v1/comments', { method: 'POST', body, prefer: 'return=minimal' }); rev.replyTo = null;
        rev.comments = await revFetch(`/rest/v1/comments?select=*,profiles!author_id(name${rev.v1 ? '' : ',avatar'})&post_id=eq.${p.id}&order=created_at.asc`); return revDrawPost();
      }
      case 'delc': if (await uiConfirm('¿Borrar comentario?', 'No se puede deshacer.', 'Borrar')) { await revFetch(`/rest/v1/comments?id=eq.${d.id}`, { method: 'DELETE' }); rev.comments = rev.comments.filter(c => c.id !== d.id); revDrawPost(); } return;
      case 'del': if (await uiConfirm('¿Eliminar este artículo?', 'Se borrarán también sus comentarios y me gusta.', 'Eliminar')) { await revFetch(`/rest/v1/posts?id=eq.${p.id}`, { method: 'DELETE' }); revUnPair(p.cover); rev.posts = []; toast('Artículo eliminado.'); revBack(); } return;
      case 'play': { const id = rev.ids[+d.i]; if (id !== undefined) playLib(r => r.id === id, id); return; }
      case 'pickcover': return $('rvCovFile').click();
      case 'nocover': rev.cover = ''; revDrawCov(); return revSaveDraft();
      case 'fmt': return revFormat(d.f);
      case 'addtr': { const r = rev.found[+d.i]; if (r && rev.tracks.length < 12 && !rev.tracks.some(t => revNk(t.name) === revNk(r.name) && revNk(t.artist) === revNk(r.artist))) { rev.tracks.push({ name: String(r.name || '').slice(0, 120), artist: String(r.artist || '').slice(0, 120), album: String(r.album || '').slice(0, 120) }); revDrawTrk(); revSaveDraft(); } return; }
      case 'deltr': rev.tracks.splice(+d.i, 1); revDrawTrk(); return revSaveDraft();
      case 'publish': return revPublish(d.v);
      case 'st': {
        const t = d.t, k = d.k, v = /^\d+$/.test(d.v) ? +d.v : d.v; if (!['st', 'pst', 'ast'].includes(t)) return;
        const pre = k === 'th' && REV_PRE[v] ? REV_PRE[v] : {};
        rev[t] = revSt({ ...rev[t], ...pre, [k]: v }); revStChanged(); return revSaveDraft();
      }
      case 'artist': return revArtist(d.id);
      case 'editartist': return revEditArtist(rev.artist);
      case 'saveartist': return revSaveArtist();
      case 'delartist': if (await uiConfirm(`¿Eliminar el perfil de ${rev.artist.name}?`, 'Los pósters y artículos no se borran.', 'Eliminar')) { await revFetch(`/rest/v1/artists?id=eq.${rev.artist.id}`, { method: 'DELETE' }); if (rev.artist.cover) revUnupload(rev.artist.cover); rev.artists = null; toast('Perfil eliminado.'); revSection('artists'); } return;
      case 'pickacover': return $('rvACovFile').click();
      case 'noacover': rev.acover = ''; $('rvACovSt').textContent = 'Sin imagen.'; return revStChanged(true);
      case 'playartist': { const n = revNk(rev.artist.name); return playLib(r => revNk(r.artist) === n); }
      case 'shareartist': return revShare(rev.artist.name, '#rva=' + rev.artist.id);
      case 'pfilter': rev.pFilter = d.v; document.querySelectorAll('#revBody .rv-tabs .rv-tab').forEach(x => x.classList.toggle('on', x.dataset.v === d.v)); return d.v === 'retos' ? revChallenges() : revPLoad(false);
      case 'pmore': return revPLoad(true);
      case 'poster': return revPosterOpen(d.i !== undefined ? rev.posters[+d.i] : d.id);
      case 'dl': return revDownload(revUrl(rev.poster.image_url), rev.poster.title);
      case 'sharep': return revShare(rev.poster.title, '#rvp=' + rev.poster.id);
      case 'delp': return revDelPoster();
      case 'csearch': return revCommons();
      case 'csave': return revCommonsSave(+d.i);
      case 'report': return revReport(d.type, d.id);
      case 'tag': rev.tagF = d.v || ''; rev.sec = 'art'; rev.tab = 'all'; rev.stack = []; revBar(); return revFeed(true);
      case 'yt': b.outerHTML = `<div class="rv-ytf"><iframe src="https://www.youtube-nocookie.com/embed/${encodeURIComponent(d.v)}?autoplay=1" title="Video" allow="autoplay; encrypted-media; picture-in-picture" allowfullscreen></iframe></div>`; return;
      case 'lightbox': return revLightbox(d.v);
      case 'search': return revSearchView();
      case 'edition': return revEdition(d.id);
      case 'editedition': return revEditEdition(rev.edition);
      case 'saveed': return revSaveEdition(d.v === '1');
      case 'deled': if (await uiConfirm('¿Eliminar esta edición?', 'Los artículos y pósters no se borran.', 'Eliminar')) { await revFetch(`/rest/v1/editions?id=eq.${rev.editE}`, { method: 'DELETE' }); rev.stack = []; return revEditions(); } return;
      case 'shareed': return revShare(rev.edition.title, '');
      case 'colsave': return revSaveToCollection();
      case 'collection': return revCollection(d.id);
      case 'colpriv': await revFetch(`/rest/v1/collections?id=eq.${rev.col.id}`, { method: 'PATCH', body: { public: !rev.col.public }, prefer: 'return=minimal' }); return revCollection(rev.col.id, true);
      case 'coldel': if (await uiConfirm('¿Eliminar esta colección?', 'Los pósters siguen en la galería.', 'Eliminar')) { await revFetch(`/rest/v1/collections?id=eq.${rev.col.id}`, { method: 'DELETE' }); toast('Colección eliminada.'); revBack(); } return;
      case 'coldelitem': await revFetch(`/rest/v1/collection_items?collection_id=eq.${rev.col.id}&poster_id=eq.${d.id}`, { method: 'DELETE' }); return revCollection(rev.col.id, true);
      case 'wall': return revWallpaper();
      case 'story': return revStoryCard(d.v);
      case 'compare': return revCompare();
      case 'cmppick': document.querySelectorAll('#revBody .rv-tabs .rv-tab').forEach(x => x.classList.toggle('on', x === b)); return rev.cmpPick && rev.cmpPick(+d.i);
      case 'aiw': return revAIWrite(d.v);
      case 'pickgal': return $('rvGalFile').click();
      case 'delgal': rev.gal.splice(+d.i, 1); revDrawGal(); return revSaveDraft();
      case 'backup': return revBackup();
      case 'notifs': return revNotifs();
      case 'rules': return revRules();
      case 'rec': return revRecommend();
      case 'read': return revRead();
      case 'playlist': return revPlayList();
      case 'reply': rev.replyTo = { id: d.id, name: d.name }; revDrawPost(); { const t = $('rvCom'); if (t) { t.focus(); t.scrollIntoView({ block: 'center' }); } } return;
      case 'noreply': rev.replyTo = null; return revDrawPost();
      case 'vote': return revVote(d.id, +d.i);
      case 'delpoll': if (await uiConfirm('¿Borrar este debate?', 'Se borran también sus votos.', 'Borrar')) { await revFetch(`/rest/v1/polls?id=eq.${d.id}`, { method: 'DELETE' }); revPolls(); } return;
      case 'chjoin': return revUploadPoster(d.id);
      case 'chvote': return revChVote(d.ch, d.id);
      case 'chnew': return revNewChallenge();
      case 'chdel': if (await uiConfirm('¿Borrar este reto?', 'Los pósters se quedan en la galería.', 'Borrar')) { await revFetch(`/rest/v1/challenges?id=eq.${d.id}`, { method: 'DELETE' }); revChallenges(); } return;
      case 'verify': await revFetch(`/rest/v1/artists?id=eq.${rev.artist.id}`, { method: 'PATCH', body: { verified: d.v === '1' }, prefer: 'return=minimal' }); toast(d.v === '1' ? 'Banda verificada ✓' : 'Verificación quitada.'); rev.artists = null; return revArtist(rev.artist.id, true);
      case 'addmgr': return revAddManager();
      case 'delmgr': await revFetch(`/rest/v1/artist_managers?artist_id=eq.${rev.artist.id}&user_id=eq.${d.id}`, { method: 'DELETE' }); return revManagers(rev.artist);
      case 'agmode': return revAgenda(d.v);
      case 'ics': return rev.events[+d.i] && revIcs(rev.events[+d.i]);
      case 'delev': if (await uiConfirm('¿Borrar este evento?', 'Se quita de la agenda.', 'Borrar')) { await revFetch(`/rest/v1/events?id=eq.${d.id}`, { method: 'DELETE' }); revAgenda(); } return;
      case 'todayp': rev.pQ = d.v; rev.pFilter = 'all'; rev.stack = []; return revPosters(true);
      case 'aname': {   // el administrador corrige el nombre de una persona (2 a 40 letras)
        const actual = (rev.prof && rev.prof.id === d.id) ? rev.prof.name : ((rev.admUsers || []).find(u => u.id === d.id) || {}).name || '';
        const v = await uiDialog({ title: 'Cambiar nombre', text: 'Es el nombre público de esta persona en la Revista (de 2 a 40 letras).', fields: [{ key: 'name', label: 'Nombre', value: actual }], ok: 'Guardar' });
        if (!v) return;
        const nombre = (v.name || '').replace(/\s+/g, ' ').trim().slice(0, 40);
        if (nombre.length < 2) return toast('El nombre debe tener al menos 2 letras.');
        await revFetch(`/rest/v1/profiles?id=eq.${d.id}`, { method: 'PATCH', body: { name: nombre }, prefer: 'return=minimal' });
        toast('Nombre cambiado.');
        if (rev.view === 'profile' && rev.prof && rev.prof.id === d.id) { rev.prof.name = nombre; revDrawProfile(); return revProfTab(rev.profTab); }
        return revAdmin(rev.admTab);
      }
      case 'ban': {
        const on = d.v === '1';
        if (!(await uiConfirm(on ? '¿Suspender esta cuenta?' : '¿Reactivar esta cuenta?', on ? 'Podrá leer, pero no publicar, comentar, dar me gusta ni seguir.' : 'Podrá volver a publicar.', on ? 'Suspender' : 'Reactivar', on))) return;
        await revFetch(`/rest/v1/profiles?id=eq.${d.id}`, { method: 'PATCH', body: { banned: on }, prefer: 'return=minimal' });
        toast(on ? 'Cuenta suspendida.' : 'Cuenta reactivada.');
        if (rev.view === 'profile') { rev.prof.banned = on; revDrawProfile(); return revProfTab(rev.profTab); }
        if (on && rev.admTab === 'reports') return revAdmClose('profile', d.id, 'resuelta', true);
        return revAdmin(rev.admTab);
      }
      case 'adm': return revAdmin(d.v);
      case 'admview': return revAdmView(d.type, d.id);
      case 'admdel': return revAdmDelete(d.type, d.id);
      case 'admclose': return revAdmClose(d.type, d.id, d.v);
      case 'admsave': return revAdmSaveSettings();
      case 'admuser': return revProfile(d.id);
    }
  } catch (e) { toast(e.message, true, 6000); }
}
{
  const v = $('view-revista');
  v.addEventListener('click', e => { const b = e.target.closest('[data-act]'); if (!b || !v.contains(b)) return; if (b.tagName === 'A') e.preventDefault(); revAct(b); });
  v.addEventListener('keydown', e => {
    if (e.key !== 'Enter') return;
    if (e.target.id === 'rvCQ') { e.preventDefault(); return revCommons(); }
    if (e.target.matches('.rv-card,.rv-acard,.rv-pcard,.rv-pmini,.rv-click')) revAct(e.target);
  });
  let pT = 0;
  v.addEventListener('input', e => {
    const id = e.target.id;
    if (id === 'rvFind') revFind();
    else if (/^rv(Title|Sub|Text)$/.test(id)) { revSaveDraft(); revStChanged(true); }
    else if (/^rv(PName|PBio|AName|AGenre|AYears|ABio)$/.test(id)) revStChanged(true);
    else if (id === 'rvMh' || id === 'rvStk') { const t = e.target.dataset.t; if (['st', 'pst', 'ast'].includes(t)) { rev[t] = revSt({ ...rev[t], [id === 'rvMh' ? 'mh' : 'stk']: e.target.value }); revStChanged(true); revSaveDraft(); } }
    else if (id === 'rvASearch') { rev.aQ = e.target.value; revDrawArtists(); }
    else if (id === 'rvPSearch') { rev.pQ = e.target.value; clearTimeout(pT); pT = setTimeout(() => revPLoad(false), 350); }
    else if (id === 'rvGQ') { clearTimeout(pT); pT = setTimeout(revSearch, 350); }
    else if (id === 'rvTags') revSaveDraft();
    else if (id === 'rvRvScore') { if ($('rvRvS')) $('rvRvS').textContent = e.target.value; }
    else if (id === 'rvUSearch') { rev.uQ = e.target.value; clearTimeout(pT); pT = setTimeout(() => revAdmUsers(), 350); }
  });
  // Si una miniatura no existe (imágenes viejas), se usa la imagen grande.
  v.addEventListener('error', e => { const t = e.target; if (!t || t.tagName !== 'IMG') return; if (t.dataset.full && t.getAttribute('src') !== t.dataset.full) t.src = t.dataset.full; else if (t.closest('.rv-yt')) t.style.visibility = 'hidden'; }, true);
  v.addEventListener('change', async e => {
    if (e.target.id === 'rvRvOn') { $('rvRvF').hidden = !e.target.checked; return; }
    if (e.target.id === 'rvGalFile') { const fs = [...e.target.files]; e.target.value = ''; for (const f of fs) { if ((rev.gal || []).length >= 8) { toast('Máximo 8 fotos.'); break; } try { rev.gal.push((await revResize(f, 1600, 0.82)).data()); } catch {} } revDrawGal(); return; }
    const id = e.target.id, f = e.target.files && e.target.files[0]; if (!f) return; e.target.value = '';
    try {
      if (id === 'rvCovFile') { rev.cover = (await revResize(f, 1400, 0.82)).data(); revDrawCov(); revSaveDraft(); }
      else if (id === 'rvAvFile') { rev.pav = (await revResize(f, 256, 0.8, true)).data(); revStChanged(true); }
      else if (id === 'rvACovFile') revArtistCover(f);
    } catch { toast('No pude leer esa imagen.', true); }
  });
  // Enlace compartido que llegó antes de cargar este archivo (lo guardó el cargador de index.html)
  if (window.revDeepInit) { rev.deep = window.revDeepInit; window.revDeepInit = null; }
}

/* ===== Diseño avanzado de artículos: fondo, difuminados, transparencias, letra y modo collage =====
   Todo se guarda como datos (nunca CSS ni HTML): cada valor sale de una lista permitida o se valida. */
const DZ_BG = {
  none: ['Ninguno', ''], cover: ['Tu portada difuminada', ''], color: ['Mis colores', ''],
  aurora: ['Aurora', 'linear-gradient(160deg,#0f2027,#2c5364 45%,#3ecf8e)'],
  atardecer: ['Atardecer', 'linear-gradient(180deg,#ff7e5f,#feb47b 45%,#6a3093)'],
  neon: ['Neón', 'radial-gradient(circle at 20% 20%,#ff3d8b 0,transparent 45%),radial-gradient(circle at 80% 70%,#29d3f0 0,transparent 45%),#0b0a14'],
  oceano: ['Océano', 'linear-gradient(200deg,#1a2a6c,#2a7ab0 50%,#a8e6cf)'],
  fuego: ['Fuego', 'linear-gradient(10deg,#200122,#6f0000 40%,#ff8a3d)'],
  noche: ['Noche estrellada', 'radial-gradient(1.2px 1.2px at 20% 30%,#fff 50%,transparent 51%) 0 0/90px 90px,radial-gradient(1.6px 1.6px at 70% 60%,#fff 50%,transparent 51%) 0 0/130px 130px,linear-gradient(#0b1026,#1c2541)'],
  grunge: ['Grunge', 'radial-gradient(ellipse at 30% 40%,rgba(224,72,154,.85),transparent 55%),radial-gradient(ellipse at 75% 65%,rgba(181,211,52,.7),transparent 50%),#15161a'],
  vinilo: ['Vinilo', 'repeating-radial-gradient(circle at 50% 50%,#111 0 2px,#1c1c1c 2px 4px)'],
  rayas: ['Rayas', 'repeating-linear-gradient(45deg,#1b1e23 0 18px,#f2c230 18px 36px)'],
  menta: ['Menta', 'linear-gradient(135deg,#d4fc79,#96e6a1)'],
  pastel: ['Arcoíris pastel', 'linear-gradient(120deg,#f6d5f7,#fbe9d7 30%,#d4f1f4 60%,#e2d1f9)'],
  cromo: ['Cromo 2000', 'linear-gradient(160deg,#e9eef5,#9fb3c8 50%,#f4f7fb)'],
  y2k: ['Pop Y2K', 'linear-gradient(135deg,#b6f23a,#29d3f0 60%,#ff8a3d)'],
  tokio: ['Tokio kawaii', 'radial-gradient(#fff 20%,transparent 21%) 0 0/24px 24px,#ffd6e8'],
  trama: ['Trama punk', 'radial-gradient(#ff3d8b 30%,transparent 31%) 0 0/14px 14px,#f1e9d8'],
  cuadros: ['Cuadros', 'conic-gradient(#111 25%,#f4f4f4 0 50%,#111 0 75%,#f4f4f4 0) 0 0/40px 40px'],
  periodico: ['Periódico', 'repeating-linear-gradient(0deg,rgba(0,0,0,.07) 0 1px,transparent 1px 22px),#efe8da']
};
const DZ_BGL = new Set(['menta', 'pastel', 'cromo', 'y2k', 'tokio', 'trama', 'periodico']);
const DZ_TF = { '': 'Del tema', sistema: 'system-ui,sans-serif', lora: 'Lora,serif', playfair: "'Playfair Display',serif", mono: "'Space Mono',monospace", caveat: 'Caveat,cursive',
  abril: "'Abril Fatface',serif", anton: 'Anton,sans-serif', shrikhand: 'Shrikhand,cursive', rampart: "'Rampart One',cursive", glitch: "'Rubik Glitch',cursive", archivo: "'Archivo Black',sans-serif" };
const DZ_TFN = { '': 'Del tema', sistema: 'Normal', lora: 'Lora', playfair: 'Elegante', mono: 'Máquina', caveat: 'Manuscrita', abril: 'Abril', anton: 'Cartel', shrikhand: 'Retro', rampart: 'Burbuja', glitch: 'Glitch', archivo: 'Bloque' };
const DZ_PANEL = { none: 'Sin panel', vidrio: 'Vidrio', negro: 'Oscuro', blanco: 'Claro', papel: 'Papel', cinta: 'Recorte' };
const DZ_CUT = { rect: 'Foto', roto: 'Papel roto', polaroid: 'Polaroid', circulo: 'Círculo', ovalo: 'Óvalo', estrella: 'Estrella', corazon: 'Corazón' };
const DZ_TXT = { recorte: 'Letras recortadas', titular: 'Titular', maquina: 'Máquina', marcador: 'Marcador', neon: 'Neón', cinta: 'Cinta' };
const DZ_STK = { estallido: 'Estallido', circulo: 'Círculo', etiqueta: 'Etiqueta', estrella: '★', chispa: '✦', corazon: '♥', rayo: '⚡', flor: '✿', nota: '♫', calavera: '☠', paz: '☮' };
const DZ_GLYPH = { estrella: '★', chispa: '✦', corazon: '♥', rayo: '⚡', flor: '✿', nota: '♫', calavera: '☠', paz: '☮' };
const DZ_SHP = { circulo: 'Círculo', franja: 'Franja', trama: 'Trama', rayas: 'Rayas', cuadros: 'Cuadros', spray: 'Spray', marco: 'Marco' };
const DZ_TAPE = { beige: 'rgba(237,229,207,.85)', rosa: 'rgba(255,128,180,.75)', azul: 'rgba(120,180,255,.7)', negra: 'rgba(20,20,20,.9)', amarilla: 'rgba(242,194,48,.8)' };
const DZ_COLS = ['#141414', '#ffffff', '#f2c230', '#ff3d8b', '#e3242b', '#29d3f0', '#3ecf8e', '#a78bfa', '#ff8a3d', '#2d47c4'];
const DZ_DEF = { bg: 'none', c1: '#ff3d8b', c2: '#29d3f0', gt: 'lin', ga: 160, blur: 18, dim: 0, vin: 0, grain: 0,
  tf: '', hf: '', fs: 0, lh: 0, ts: 0, tc: '', hc: '', ta: '', tsh: 0, panel: 'none', pop: 70, pbl: 10, rad: 16, cpos: 'fondo', ch: 'vertical', col: [] };
const dzHex = v => typeof v === 'string' && /^#[0-9a-f]{6}$/i.test(v) ? v.toLowerCase() : '';
const dzNum = (v, a, b, d) => { const n = Number(v); return Number.isFinite(n) ? Math.min(b, Math.max(a, Math.round(n * 10) / 10)) : d; };
const dzOne = (v, o, d) => typeof v === 'string' && Object.hasOwn(o, v) ? v : d;
function dzItem(i) {
  if (!i || typeof i !== 'object') return null;
  const k = ['img', 'txt', 'stk', 'tape', 'shp'].includes(i.k) ? i.k : null; if (!k) return null;
  const it = { k, x: dzNum(i.x, -20, 120, 50), y: dzNum(i.y, -20, 120, 50), w: dzNum(i.w, 2, 140, 30), r: dzNum(i.r, -180, 180, 0), z: dzNum(i.z, 0, 99, 1) | 0, o: dzNum(i.o, 10, 100, 100) };
  const c = dzHex(i.c); if (c) it.c = c;
  if (k === 'img') { const s = revPic(i.s); if (!s) return null; it.s = s; it.cut = dzOne(i.cut, DZ_CUT, 'rect'); }
  else if (k === 'txt') { it.t = revClean(String(i.t || '').replace(/\r/g, ''), 60) || 'Texto'; it.v = dzOne(i.v, DZ_TXT, 'recorte'); }
  else if (k === 'stk') { it.v = dzOne(i.v, DZ_STK, 'estallido'); it.t = revClean(String(i.t || ''), 24); }
  else if (k === 'tape') it.v = dzOne(i.v, DZ_TAPE, 'beige');
  else it.v = dzOne(i.v, DZ_SHP, 'circulo');
  return it;
}
// revClean quita saltos de línea; para el texto del collage se permiten (máximo 3 líneas)
function dzCleanTxt(t) { return String(t || '').replace(/[\u0000-\u0009\u000b-\u001f<>]/g, '').split('\n').slice(0, 3).join('\n').trim().slice(0, 60); }
function dzClean(o) {
  o = o && typeof o === 'object' ? o : {}; const d = { ...DZ_DEF };
  d.bg = dzOne(o.bg, DZ_BG, 'none'); d.c1 = dzHex(o.c1) || DZ_DEF.c1; d.c2 = dzHex(o.c2) || DZ_DEF.c2; d.gt = ['lin', 'rad', 'con'].includes(o.gt) ? o.gt : 'lin';
  d.ga = dzNum(o.ga, 0, 360, 160) | 0; d.blur = dzNum(o.blur, 0, 40, 18) | 0; d.dim = dzNum(o.dim, 0, 90, 0) | 0; d.vin = o.vin ? 1 : 0; d.grain = o.grain ? 1 : 0;
  d.tf = dzOne(o.tf, DZ_TF, ''); d.hf = dzOne(o.hf, DZ_TF, ''); d.fs = o.fs ? dzNum(o.fs, 13, 28, 0) | 0 : 0; d.lh = o.lh ? dzNum(o.lh, 1.2, 2.2, 0) : 0; d.ts = o.ts ? dzNum(o.ts, 20, 80, 0) | 0 : 0;
  d.tc = dzHex(o.tc); d.hc = dzHex(o.hc); d.ta = ['', 'l', 'j', 'c'].includes(o.ta) ? o.ta : ''; d.tsh = o.tsh ? 1 : 0;
  d.panel = dzOne(o.panel, DZ_PANEL, 'none'); d.pop = dzNum(o.pop, 0, 100, 70) | 0; d.pbl = dzNum(o.pbl, 0, 30, 10) | 0; d.rad = dzNum(o.rad, 0, 40, 16) | 0;
  d.cpos = o.cpos === 'cabecera' ? 'cabecera' : 'fondo'; d.ch = ['vertical', 'cuadrado', 'horizontal'].includes(o.ch) ? o.ch : 'vertical';
  d.col = (Array.isArray(o.col) ? o.col : []).slice(0, 30).map(i => { const it = dzItem(i); if (it && it.k === 'txt') it.t = dzCleanTxt(i.t) || 'Texto'; return it; }).filter(Boolean);
  return d;
}
// Solo lo que cambió (para guardar poco)
function dzDiff(d) { const r = {}; Object.keys(DZ_DEF).forEach(k => { if (k === 'col') { if (d.col.length) r.col = d.col; } else if (d[k] !== DZ_DEF[k]) r[k] = d[k]; }); return r; }
const dzEmpty = d => !Object.keys(dzDiff(d)).length;
function dzLum(h) { const n = parseInt(h.slice(1), 16); return ((n >> 16 & 255) * .299 + (n >> 8 & 255) * .587 + (n & 255) * .114) / 255; }
function dzGrad(d) {
  return d.gt === 'rad' ? `radial-gradient(circle at 30% 20%,${d.c1},${d.c2})` : d.gt === 'con' ? `conic-gradient(from ${d.ga}deg,${d.c1},${d.c2},${d.c1})` : `linear-gradient(${d.ga}deg,${d.c1},${d.c2})`;
}
const DZ_GRAIN = "url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='160' height='160'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.9' numOctaves='2' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='.55'/%3E%3C/svg%3E\")";
const DZ_RECORTE = [['#141414', '#fff', "'Archivo Black',sans-serif"], ['#fff', '#141414', "'Playfair Display',serif"], ['#ff3d8b', '#141414', 'Anton,sans-serif'], ['#f2c230', '#141414', "'Abril Fatface',serif"], ['#e3242b', '#fff', "'Space Mono',monospace"], ['#f1e9d8', '#141414', 'Lora,serif']];
function dzItemHtml(it, i) {
  const st = `left:${it.x}%;top:${it.y}%;width:${it.k === 'txt' ? 'auto' : it.w + '%'};transform:translate(-50%,-50%) rotate(${it.r}deg);z-index:${it.z};opacity:${it.o / 100}`;
  const c = it.c || '';
  let inner = '';
  if (it.k === 'img') inner = `<img src="${esc(it.s)}" alt="" loading="lazy" decoding="async" draggable="false">`;
  else if (it.k === 'txt') {
    const fz = `font-size:${(it.w / 6).toFixed(2)}cqw`;
    if (it.v === 'recorte') {
      let n = 0;
      inner = `<span class="dz-rec" style="${fz}">${[...it.t].map(ch => { if (ch === '\n') return '<br>'; if (ch === ' ') return '<i class="dz-sp"></i>'; const p = DZ_RECORTE[(n * 7 + ch.charCodeAt(0)) % DZ_RECORTE.length], g = ((n++ * 37) % 11) - 5; return `<b style="background:${p[0]};color:${p[1]};font-family:${p[2]};transform:rotate(${g}deg)">${esc(ch)}</b>`; }).join('')}</span>`;
    } else inner = `<span class="dz-t dz-t-${it.v}" style="${fz}${c ? `;--c:${c}` : ''}">${esc(it.t).replace(/\n/g, '<br>')}</span>`;
  } else if (it.k === 'stk') {
    const g = DZ_GLYPH[it.v];
    inner = g ? `<span class="dz-glyph" style="font-size:${(it.w * 0.8).toFixed(1)}cqw;color:${c || '#ff3d8b'}">${g}</span>` : `<span class="dz-stk dz-stk-${it.v}" style="--c:${c || '#f2c230'};font-size:${(it.w / 7).toFixed(2)}cqw"><span>${esc(it.t || '¡Nuevo!')}</span></span>`;
  } else if (it.k === 'tape') inner = `<i class="dz-tape" style="background:${DZ_TAPE[it.v]}"></i>`;
  else inner = `<i class="dz-shp dz-shp-${it.v}" style="--c:${c || '#ff3d8b'}"></i>`;
  return `<div class="dz-it dz-${it.k}${it.k === 'img' ? ' dz-cut-' + it.cut : ''}" data-i="${i}" style="${st}">${inner}</div>`;
}
const dzColHtml = col => col.map(dzItemHtml).join('');
// Aplica el diseño a un artículo (o a la vista previa del editor).
function dzApply(el, raw, cover) {
  if (!el) return;
  [...el.classList].filter(c => c === 'rv-dz' || c.startsWith('dz-')).forEach(c => el.classList.remove(c));
  el.querySelectorAll(':scope > .dz-bg, :scope > .dz-cab').forEach(x => x.remove());
  ['--dz-tf', '--dz-hf', '--dz-fs', '--dz-lh', '--dz-ts', '--dz-tc', '--dz-hc', '--dz-pop', '--dz-pbl', '--dz-rad'].forEach(k => el.style.removeProperty(k));
  const d = dzClean(raw); if (dzEmpty(d)) return;
  revFonts(); el.classList.add('rv-dz');
  let bgCss = '', light = null;
  const im = d.bg === 'cover' ? revPic(cover) : '';
  if (d.bg === 'color') { bgCss = dzGrad(d); light = (dzLum(d.c1) + dzLum(d.c2)) / 2 > 0.6; }
  else if (d.bg === 'cover') light = im ? false : null;
  else if (d.bg !== 'none') { bgCss = DZ_BG[d.bg][1]; light = DZ_BGL.has(d.bg); }
  if (light !== null && d.dim >= 35) light = false;
  const colFondo = d.col.length && d.cpos === 'fondo';
  if (bgCss || im || colFondo || d.vin || d.grain || d.dim) {
    const bg = document.createElement('div'); bg.className = 'dz-bg'; bg.setAttribute('aria-hidden', 'true');
    bg.innerHTML = `<div class="dz-stick">${bgCss ? `<i class="dz-fill" style="background:${bgCss}"></i>` : ''}${im ? `<img class="dz-fill dz-bimg" src="${esc(im)}" alt="" style="filter:blur(${d.blur}px)">` : ''}${colFondo ? `<div class="dz-col dz-colf">${dzColHtml(d.col)}</div>` : ''}${d.dim ? `<i class="dz-fill" style="background:rgba(0,0,0,${d.dim / 100})"></i>` : ''}${d.vin ? '<i class="dz-fill dz-vin"></i>' : ''}${d.grain ? `<i class="dz-fill dz-grain" style="background-image:${DZ_GRAIN}"></i>` : ''}</div>`;
    el.prepend(bg); el.classList.add('dz-hasbg');
  }
  if (d.col.length && d.cpos === 'cabecera') {
    const c = document.createElement('div'); c.className = `dz-cab dz-col dz-a-${d.ch}`; c.setAttribute('aria-hidden', 'true'); c.innerHTML = dzColHtml(d.col);
    const b = el.querySelector(':scope > .dz-bg'); if (b) b.after(c); else el.prepend(c);
  }
  const S = (k, v) => el.style.setProperty(k, v);
  if (d.tf) { el.classList.add('dz-tf'); S('--dz-tf', DZ_TF[d.tf]); }
  if (d.hf) { el.classList.add('dz-hf'); S('--dz-hf', DZ_TF[d.hf]); }
  if (d.fs) { el.classList.add('dz-fs'); S('--dz-fs', d.fs + 'px'); }
  if (d.lh) { el.classList.add('dz-lh'); S('--dz-lh', String(d.lh)); }
  if (d.ts) { el.classList.add('dz-ts'); S('--dz-ts', d.ts + 'px'); }
  if (d.ta) el.classList.add('dz-ta-' + d.ta);
  if (d.tsh) el.classList.add('dz-tsh');
  const hc = d.hc || (light === null ? '' : light ? '#141414' : '#f4f4f4');
  const tc = d.tc || (['papel', 'blanco'].includes(d.panel) ? '#1b1b1b' : ['negro', 'vidrio'].includes(d.panel) && light !== true ? '#f2f2f2' : d.panel === 'vidrio' && light ? '#141414' : hc);
  if (hc) { el.classList.add('dz-hc'); S('--dz-hc', hc); }
  if (tc) { el.classList.add('dz-tc'); S('--dz-tc', tc); }
  if (d.panel !== 'none') { el.classList.add('dz-p', 'dz-p-' + d.panel); S('--dz-pop', String(d.pop / 100)); S('--dz-pbl', d.pbl + 'px'); S('--dz-rad', d.rad + 'px'); }
}
document.head.insertAdjacentHTML('beforeend', `<style id="dz-css">
.rv-dz{position:relative;isolation:isolate}
.rv-dz.dz-hasbg{padding:14px;border-radius:18px}
.dz-bg{position:absolute;inset:0;z-index:-1;overflow:clip;border-radius:inherit;pointer-events:none}
.dz-stick{position:sticky;top:0;height:100vh;max-height:100%;width:100%;overflow:clip}
.dz-fill{position:absolute;inset:0;display:block}
img.dz-bimg{width:100%;height:100%;object-fit:cover;transform:scale(1.15)}
.dz-vin{background:radial-gradient(ellipse at center,transparent 45%,rgba(0,0,0,.65))}
.dz-grain{mix-blend-mode:overlay;opacity:.5;background-size:160px 160px}
.dz-col{position:relative;container-type:size;overflow:clip}
.dz-colf{position:absolute;inset:0}
.dz-cab{width:100%;border-radius:14px;margin:0 0 12px;background:transparent}
.dz-a-vertical{aspect-ratio:9/16;max-height:80vh}.dz-a-cuadrado{aspect-ratio:1}.dz-a-horizontal{aspect-ratio:16/9}
.dz-it{position:absolute;line-height:1;user-select:none;-webkit-user-select:none}
.dz-img img{display:block;width:100%;height:auto;box-shadow:0 6px 18px rgba(0,0,0,.35)}
.dz-cut-roto img{clip-path:polygon(0% 3%,8% 0%,17% 4%,26% 1%,35% 3%,46% 0%,57% 4%,68% 1%,79% 3%,90% 0%,100% 2%,98% 14%,100% 27%,97% 40%,100% 53%,98% 66%,100% 79%,97% 90%,100% 100%,89% 97%,78% 100%,66% 96%,55% 100%,44% 97%,33% 100%,22% 96%,11% 100%,0% 97%,2% 85%,0% 72%,3% 60%,0% 47%,2% 34%,0% 21%,3% 10%);box-shadow:none;filter:drop-shadow(0 5px 8px rgba(0,0,0,.4))}
.dz-cut-polaroid{background:#fbfaf6;padding:4% 4% 16%;box-shadow:0 8px 20px rgba(0,0,0,.35)}.dz-cut-polaroid img{box-shadow:none}
.dz-cut-circulo img,.dz-cut-ovalo img,.dz-cut-estrella img,.dz-cut-corazon img{aspect-ratio:1;object-fit:cover;box-shadow:none}
.dz-cut-circulo img{clip-path:circle(50%)}.dz-cut-ovalo img{aspect-ratio:3/4;clip-path:ellipse(50% 50%)}
.dz-cut-estrella img{clip-path:polygon(50% 0,61% 35%,98% 35%,68% 57%,79% 91%,50% 70%,21% 91%,32% 57%,2% 35%,39% 35%)}
.dz-cut-corazon img{clip-path:polygon(50% 18%,61% 5%,75% 1%,90% 7%,99% 22%,97% 40%,85% 58%,50% 95%,15% 58%,3% 40%,1% 22%,10% 7%,25% 1%,39% 5%)}
.dz-rec{display:inline-block;white-space:nowrap;text-align:center}.dz-rec b{display:inline-block;padding:.08em .18em;margin:.04em;font-weight:900;box-shadow:0 2px 3px rgba(0,0,0,.3)}.dz-rec .dz-sp{display:inline-block;width:.4em}
.dz-t{display:inline-block;white-space:nowrap;text-align:center}
.dz-t-titular{font-family:Anton,sans-serif;text-transform:uppercase;color:var(--c,#141414);background:#f2c230;padding:.05em .25em;letter-spacing:.02em}
.dz-t-maquina{font-family:'Space Mono',monospace;color:var(--c,#141414);background:#fff;padding:.15em .4em;box-shadow:0 2px 4px rgba(0,0,0,.3)}
.dz-t-marcador{font-family:Caveat,cursive;color:var(--c,#ff3d8b);font-weight:600}
.dz-t-neon{font-family:Shrikhand,cursive;color:#fff;text-shadow:0 0 .12em var(--c,#29d3f0),0 0 .35em var(--c,#29d3f0),0 0 .7em var(--c,#29d3f0)}
.dz-t-cinta{font-family:'Archivo Black',sans-serif;color:#fff;background:var(--c,#141414);padding:.12em .5em;transform:skewX(-8deg)}
.dz-glyph{display:block;text-align:center;line-height:1;text-shadow:0 3px 6px rgba(0,0,0,.35)}
.dz-stk{display:flex;align-items:center;justify-content:center;aspect-ratio:1;background:var(--c);color:#141414;font-family:'Archivo Black',sans-serif;text-transform:uppercase;text-align:center;line-height:1.05;padding:12%}
.dz-stk-estallido{clip-path:polygon(50% 0,58% 15%,72% 5%,74% 21%,90% 16%,85% 32%,100% 35%,89% 48%,100% 60%,85% 65%,92% 82%,75% 79%,72% 96%,59% 86%,50% 100%,41% 86%,28% 96%,25% 79%,8% 82%,15% 65%,0% 60%,11% 48%,0% 35%,15% 32%,10% 16%,26% 21%,28% 5%,42% 15%)}
.dz-stk-circulo{border-radius:50%;border:.25em dashed rgba(0,0,0,.55)}.dz-stk-etiqueta{aspect-ratio:2.6;border-radius:.4em;box-shadow:0 3px 6px rgba(0,0,0,.35)}
.dz-tape{display:block;width:100%;aspect-ratio:3.6;clip-path:polygon(2% 0,98% 0,100% 15%,97% 30%,100% 50%,97% 70%,100% 85%,98% 100%,2% 100%,0 85%,3% 70%,0 50%,3% 30%,0 15%)}
.dz-shp{display:block;width:100%}
.dz-shp-circulo{aspect-ratio:1;border-radius:50%;background:var(--c)}
.dz-shp-franja{aspect-ratio:5;background:var(--c)}
.dz-shp-trama{aspect-ratio:1;border-radius:50%;background:radial-gradient(var(--c) 32%,transparent 33%) 0 0/12% 12%;mask:radial-gradient(circle,#000 40%,transparent 72%);-webkit-mask:radial-gradient(circle,#000 40%,transparent 72%)}
.dz-shp-rayas{aspect-ratio:1.4;background:repeating-linear-gradient(45deg,var(--c) 0 8%,transparent 8% 16%)}
.dz-shp-cuadros{aspect-ratio:1.4;background:conic-gradient(var(--c) 25%,transparent 0 50%,var(--c) 0 75%,transparent 0) 0 0/20% 28%}
.dz-shp-spray{aspect-ratio:1;border-radius:50%;background:radial-gradient(circle,var(--c) 0,transparent 68%);filter:blur(4px)}
.dz-shp-marco{aspect-ratio:.75;border:.9em solid var(--c);box-shadow:inset 0 0 0 2px rgba(0,0,0,.2)}
.rv-dz.dz-tf .rv-txt{font-family:var(--dz-tf)!important}
.rv-dz.dz-hf h2,.rv-dz.dz-hf .rv-txt h3,.rv-dz.dz-hf .rv-txt h4{font-family:var(--dz-hf)!important}
.rv-dz.dz-fs .rv-txt{font-size:var(--dz-fs)!important}.rv-dz.dz-fs .rv-txt p,.rv-dz.dz-fs .rv-txt li{font-size:inherit!important}
.rv-dz.dz-lh .rv-txt,.rv-dz.dz-lh .rv-txt p{line-height:var(--dz-lh)!important}
.rv-dz.dz-ts h2{font-size:var(--dz-ts)!important;line-height:1.05!important}
.rv-dz.dz-ta-j .rv-txt p{text-align:justify!important}.rv-dz.dz-ta-c .rv-txt{text-align:center!important}.rv-dz.dz-ta-l .rv-txt{text-align:left!important}
.rv-dz.dz-tsh h2,.rv-dz.dz-tsh .rv-txt{text-shadow:0 2px 8px rgba(0,0,0,.55)}
.rv-dz.dz-hc>h2,.rv-dz.dz-hc>.rv-sub,.rv-dz.dz-hc>.rv-by,.rv-dz.dz-hc>.rv-by *,.rv-dz.dz-hc .rv-ov h2{color:var(--dz-hc)!important}
.rv-dz.dz-tc .rv-txt,.rv-dz.dz-tc .rv-txt *{color:var(--dz-tc)!important}
.rv-dz.dz-p .rv-txt{padding:16px 18px;border-radius:var(--dz-rad);backdrop-filter:blur(var(--dz-pbl));-webkit-backdrop-filter:blur(var(--dz-pbl))}
.rv-dz.dz-p-vidrio .rv-txt{background:rgba(255,255,255,calc(var(--dz-pop) * .22));border:1px solid rgba(255,255,255,calc(var(--dz-pop) * .4))}
.rv-dz.dz-p-negro .rv-txt{background:rgba(12,12,14,var(--dz-pop))}
.rv-dz.dz-p-blanco .rv-txt{background:rgba(255,255,255,var(--dz-pop))}
.rv-dz.dz-p-papel .rv-txt{background:rgba(241,233,216,var(--dz-pop));box-shadow:0 6px 18px rgba(0,0,0,.3)}
.rv-dz.dz-p-cinta .rv-txt{background:rgba(250,248,240,var(--dz-pop));border-radius:2px;box-shadow:0 6px 18px rgba(0,0,0,.35);clip-path:polygon(0 1%,6% 0,14% 1.2%,25% 0,37% 1%,49% 0,61% 1.3%,73% 0,85% 1%,94% 0,100% 1%,99.4% 50%,100% 99%,92% 100%,81% 98.8%,69% 100%,57% 99%,45% 100%,33% 98.9%,21% 100%,9% 99%,0 100%,.6% 50%)}
#rvDzUI{margin:6px 0 14px;border:1px solid rgba(255,255,255,.12);border-radius:14px;padding:12px}
#rvDzUI .dz-tabs{display:flex;gap:6px;overflow-x:auto;margin-bottom:10px}
#rvDzUI .dz-tabs button,#dzEd .dz-chip,#rvDzUI .dz-chip{background:transparent;border:1px solid rgba(255,255,255,.22);color:inherit;border-radius:999px;padding:6px 12px;min-height:36px;white-space:nowrap;font-size:14px}
#rvDzUI .dz-tabs button.on,#rvDzUI .dz-chip.on,#dzEd .dz-chip.on{background:var(--line,#f2c230);color:#1b1e23;border-color:transparent}
#rvDzUI .dz-row,#dzEd .dz-row{display:flex;flex-wrap:wrap;gap:6px;margin:6px 0 10px}
#rvDzUI .dz-sw{width:22px;height:22px;border-radius:6px;display:inline-block;vertical-align:middle;margin-right:6px;border:1px solid rgba(255,255,255,.3)}
#rvDzUI label.dz-l,#dzEd label.dz-l{display:flex;align-items:center;gap:10px;margin:8px 0;font-size:14px}
#rvDzUI label.dz-l input[type=range],#dzEd label.dz-l input[type=range]{flex:1}
#rvDzUI .dz-dot,#dzEd .dz-dot{width:30px;height:30px;border-radius:50%;border:2px solid rgba(255,255,255,.35);padding:0;min-height:0}
#rvDzUI .dz-dot.on,#dzEd .dz-dot.on{outline:3px solid var(--line,#f2c230);outline-offset:2px}
#rvDzUI input[type=color],#dzEd input[type=color]{width:44px;height:34px;border:0;background:none;padding:0}
#dzEd{position:fixed;inset:0;z-index:2400;background:#101114;color:#eee;display:flex;flex-direction:column;font:15px system-ui,sans-serif}
#dzEd .dz-top{display:flex;gap:8px;align-items:center;padding:calc(env(safe-area-inset-top,0px) + 8px) 10px 8px;border-bottom:1px solid #2a2d33}
#dzEd .dz-top b{flex:1}
#dzEd button{font:inherit;border:0;border-radius:10px;padding:8px 12px;min-height:40px;background:#262a31;color:#eee}
#dzEd button.dz-ok{background:var(--line,#f2c230);color:#1b1e23;font-weight:700}
#dzEd .dz-stage{flex:1;min-height:0;display:flex;align-items:center;justify-content:center;padding:10px;overflow:hidden;background:repeating-conic-gradient(#1a1c20 0 25%,#15171a 0 50%) 0 0/24px 24px}
#dzEd .dz-canvas{position:relative;height:100%;max-width:100%;box-shadow:0 10px 30px rgba(0,0,0,.6);touch-action:none}
#dzEd .dz-canvas .dz-it{cursor:grab;touch-action:none}
#dzEd .dz-canvas .dz-it.sel{outline:2px dashed var(--line,#f2c230);outline-offset:4px}
#dzEd .dz-bar{padding:8px 10px calc(env(safe-area-inset-bottom,0px) + 8px);border-top:1px solid #2a2d33;max-height:42vh;overflow-y:auto}
#dzEd .dz-add{display:flex;gap:6px;overflow-x:auto;padding-bottom:4px}
#dzEd textarea{width:100%;box-sizing:border-box;background:#1b1e23;color:#eee;border:1px solid #333;border-radius:10px;padding:8px;font:inherit}
</style>`);

/* --- Editor: pestañas Fondo · Texto · Panel · Collage --- */
let dzOrig = '{}';
function dzInit(p, d) {
  rev.dz = dzClean(p ? p.design : d && d.dz); dzOrig = JSON.stringify(dzDiff(rev.dz)); rev.dzTab = rev.dzTab || 'fondo';
  rev.dzOk = !(p && p.design === undefined); // si el artículo vino sin la columna, la base aún no tiene la mejora v6
  const box = $('rvDzUI'); if (!box) return;
  box.addEventListener('click', dzUIClick); box.addEventListener('input', dzUIInput);
  dzUI(); dzPrev();
}
const dzDraft = () => rev.dz ? dzDiff(rev.dz) : null;
function dzPrev() { if (rev.view === 'edit' && rev.dz) dzApply($('rvPrev'), rev.dz, rev.cover); }
function dzUI() {
  const box = $('rvDzUI'); if (!box || !rev.dz) return; const d = rev.dz, t = rev.dzTab;
  const chip = (k, v, lab, extra = '') => `<button type="button" class="dz-chip${d[k] === v ? ' on' : ''}" data-dzs="${k}" data-v="${esc(String(v))}">${extra}${lab}</button>`;
  const rng = (k, lab, a, b, st = 1, suf = '') => `<label class="dz-l">${lab}<input type="range" min="${a}" max="${b}" step="${st}" value="${d[k] || (k === 'fs' ? 17 : k === 'lh' ? 1.6 : k === 'ts' ? 34 : 0)}" data-dzk="${k}"><output>${d[k] || !/^(fs|lh|ts)$/.test(k) ? d[k] + suf : 'auto'}</output></label>`;
  const dots = (k) => `<div class="dz-row"><button type="button" class="dz-chip${d[k] ? '' : ' on'}" data-dzs="${k}" data-v="">Auto</button>${DZ_COLS.map(c => `<button type="button" class="dz-dot${d[k] === c ? ' on' : ''}" style="background:${c}" data-dzs="${k}" data-v="${c}" aria-label="${c}"></button>`).join('')}<input type="color" value="${d[k] || '#ffffff'}" data-dzk="${k}" aria-label="Otro color"></div>`;
  let h = `<div class="dz-tabs">${[['fondo', 'Fondo'], ['texto', 'Letra'], ['panel', 'Transparencia'], ['collage', 'Collage']].map(([k, n]) => `<button type="button" class="${t === k ? 'on' : ''}" data-dzt="${k}">${n}</button>`).join('')}</div>`;
  if (t === 'fondo') {
    h += `<p class="hint">El fondo que se ve detrás del artículo al abrirlo.</p><div class="dz-row">${Object.keys(DZ_BG).map(k => chip('bg', k, DZ_BG[k][0], DZ_BG[k][1] ? `<i class="dz-sw" style="background:${DZ_BG[k][1]}"></i>` : '')).join('')}</div>`;
    if (d.bg === 'color') h += `<div class="dz-row"><label class="dz-l">Color 1 <input type="color" value="${d.c1}" data-dzk="c1"></label><label class="dz-l">Color 2 <input type="color" value="${d.c2}" data-dzk="c2"></label></div><div class="dz-row">${chip('gt', 'lin', 'Difuminado lineal')}${chip('gt', 'rad', 'Radial')}${chip('gt', 'con', 'Cónico')}</div>${d.gt !== 'rad' ? rng('ga', 'Ángulo', 0, 360, 5, '°') : ''}`;
    if (d.bg === 'cover') h += rng('blur', 'Desenfoque', 0, 40, 1, 'px') + (rev.cover ? '' : '<p class="hint">Primero elige una imagen de portada.</p>');
    h += rng('dim', 'Oscurecer', 0, 90, 5, '%') + `<div class="dz-row">${chip('vin', d.vin ? 0 : 1, 'Viñeta').replace(' on', '').replace('class="dz-chip', `class="dz-chip${d.vin ? ' on' : ''}`)}${chip('grain', d.grain ? 0 : 1, 'Grano de foto').replace(' on', '').replace('class="dz-chip', `class="dz-chip${d.grain ? ' on' : ''}`)}</div>`;
  } else if (t === 'texto') {
    h += `<p class="dz-l"><b>Letra del texto</b></p><div class="dz-row">${Object.keys(DZ_TF).map(k => chip('tf', k, DZ_TFN[k])).join('')}</div>
<p class="dz-l"><b>Letra del título</b></p><div class="dz-row">${Object.keys(DZ_TF).map(k => chip('hf', k, DZ_TFN[k])).join('')}</div>
${rng('fs', 'Tamaño del texto', 13, 28, 1, 'px')}${rng('lh', 'Interlineado', 1.2, 2.2, 0.1)}${rng('ts', 'Tamaño del título', 20, 80, 1, 'px')}
<div class="dz-row">${chip('ta', '', 'Alineación del tema')}${chip('ta', 'l', 'Izquierda')}${chip('ta', 'j', 'Justificado')}${chip('ta', 'c', 'Centrado')}${chip('tsh', d.tsh ? 0 : 1, 'Sombra en las letras').replace(' on', '').replace('class="dz-chip', `class="dz-chip${d.tsh ? ' on' : ''}`)}</div>
<p class="dz-l"><b>Color del texto</b></p>${dots('tc')}<p class="dz-l"><b>Color del título</b></p>${dots('hc')}
<div class="dz-row"><button type="button" class="dz-chip" data-dzreset="texto">Volver a la letra del tema</button></div>`;
  } else if (t === 'panel') {
    h += `<p class="hint">Un panel detrás del texto para que se lea bien sobre el fondo.</p><div class="dz-row">${Object.keys(DZ_PANEL).map(k => chip('panel', k, DZ_PANEL[k])).join('')}</div>${d.panel !== 'none' ? rng('pop', 'Opacidad', 0, 100, 5, '%') + rng('pbl', 'Desenfoque detrás', 0, 30, 1, 'px') + rng('rad', 'Bordes redondeados', 0, 40, 1, 'px') : ''}`;
  } else {
    h += `<p class="hint">Pega recortes, fotos, letras de revista, stickers y cintas. Arrástralos con el dedo; con dos dedos los agrandas y giras.</p>
<div class="dz-row">${chip('cpos', 'fondo', 'Collage de fondo')}${chip('cpos', 'cabecera', 'Collage arriba del artículo')}</div>
${d.cpos === 'cabecera' ? `<div class="dz-row">${chip('ch', 'vertical', 'Vertical')}${chip('ch', 'cuadrado', 'Cuadrado')}${chip('ch', 'horizontal', 'Horizontal')}</div>` : ''}
<div class="dz-row"><button type="button" data-dzcol="1">${ICO('image')} Abrir modo collage${d.col.length ? ` (${d.col.length})` : ''}</button>${d.col.length ? '<button type="button" class="alt" data-dzcol="0">Borrar collage</button>' : ''}</div>`;
  }
  if (!rev.dzOk) h += '<p class="hint">Para guardar el diseño avanzado, el administrador debe ejecutar <b>revista_supabase_v6.sql</b> en Supabase.</p>';
  box.innerHTML = h;
}
function dzChanged(redraw) { if (redraw) dzUI(); dzPrev(); revSaveDraft(); }
function dzUIClick(e) {
  const b = e.target.closest('button'); if (!b) return; const d = rev.dz;
  if (b.dataset.dzt) { rev.dzTab = b.dataset.dzt; return dzUI(); }
  if (b.dataset.dzs) { const k = b.dataset.dzs, v = b.dataset.v; const n = { ...d, [k]: /^(vin|grain|tsh)$/.test(k) ? Number(v) : v }; rev.dz = dzClean({ ...n, col: d.col }); rev.dz.col = d.col; return dzChanged(true); }
  if (b.dataset.dzreset === 'texto') { rev.dz = { ...d, tf: '', hf: '', fs: 0, lh: 0, ts: 0, tc: '', hc: '', ta: '', tsh: 0 }; return dzChanged(true); }
  if (b.dataset.dzcol === '1') return dzCollage();
  if (b.dataset.dzcol === '0') { uiConfirm('Borrar collage', '¿Quitar todos los recortes del collage?', 'Borrar', true).then(ok => { if (ok) { rev.dz.col = []; dzChanged(true); } }); }
}
function dzUIInput(e) {
  const k = e.target.dataset.dzk; if (!k) return; const v = e.target.value;
  const col = rev.dz.col; rev.dz = dzClean({ ...rev.dz, [k]: e.target.type === 'range' ? Number(v) : v }); rev.dz.col = col;
  const o = e.target.parentElement && e.target.parentElement.querySelector('output'); if (o) o.textContent = rev.dz[k] + ({ ga: '°', blur: 'px', dim: '%', fs: 'px', ts: 'px', pop: '%', pbl: 'px', rad: 'px' }[k] || '');
  if (e.target.type === 'color' && /^(tc|hc)$/.test(k)) e.target.parentElement.querySelectorAll('.dz-dot,.dz-chip').forEach(x => x.classList.remove('on'));
  dzPrev(); revSaveDraft();
}

/* --- Modo collage (pantalla completa) --- */
function dzCollage() {
  const d = rev.dz, col = JSON.parse(JSON.stringify(d.col)); let sel = -1;
  const ar = d.cpos === 'cabecera' ? ({ vertical: '9/16', cuadrado: '1/1', horizontal: '16/9' }[d.ch]) : '9/16';
  const ed = document.createElement('div'); ed.id = 'dzEd'; ed.setAttribute('role', 'dialog'); ed.setAttribute('aria-label', 'Modo collage');
  ed.innerHTML = `<div class="dz-top"><button type="button" data-z="cancel">Cancelar</button><b>Collage</b><button type="button" class="dz-ok" data-z="ok">Listo</button></div>
<div class="dz-stage"><div class="dz-canvas dz-col" style="aspect-ratio:${ar}"></div></div><div class="dz-bar"></div><input type="file" accept="image/*" multiple hidden>`;
  document.body.appendChild(ed); revFonts();
  const cv = ed.querySelector('.dz-canvas'), bar = ed.querySelector('.dz-bar'), file = ed.querySelector('input[type=file]');
  // Fondo del lienzo: el mismo fondo del artículo para ver cómo queda
  const prevBg = () => { const t = rev.dz; cv.style.background = t.bg === 'color' ? dzGrad(t) : t.bg === 'cover' && revPic(rev.cover) ? `center/cover url("${revPic(rev.cover)}")` : (DZ_BG[t.bg] && DZ_BG[t.bg][1]) || '#2a2d33'; };
  prevBg();
  // En pantallas anchas el lienzo no debe pasarse del ancho
  const fit = () => { const s = ed.querySelector('.dz-stage'), [a, b] = ar.split('/').map(Number); const W = s.clientWidth - 20, H = s.clientHeight - 20; if (W / H < a / b) { cv.style.height = 'auto'; cv.style.width = W + 'px'; } else { cv.style.width = 'auto'; cv.style.height = H + 'px'; } };
  const draw = () => { cv.innerHTML = dzColHtml(col); if (sel >= 0 && cv.children[sel]) cv.querySelector(`[data-i="${sel}"]`).classList.add('sel'); };
  const topZ = () => col.reduce((m, it) => Math.max(m, it.z), 0) + 1;
  const add = it => { if (col.length >= 30) return toast('Máximo 30 recortes.'); const n = dzItem({ x: Math.round(32 + Math.random() * 36), y: Math.round(28 + Math.random() * 40), r: Math.round(Math.random() * 10 - 5), z: topZ(), o: 100, ...it }); if (!n) return; if (n.k === 'txt') n.t = dzCleanTxt(it.t) || 'Texto'; col.push(n); sel = col.length - 1; draw(); panel(); };
  const opts = (k, o, cur) => Object.keys(o).map(v => `<button type="button" class="dz-chip${cur === v ? ' on' : ''}" data-zk="${k}" data-v="${v}">${typeof o[v] === 'string' && !o[v].startsWith('rgba') ? o[v] : v}</button>`).join('');
  const colors = cur => DZ_COLS.map(c => `<button type="button" class="dz-dot${cur === c ? ' on' : ''}" style="background:${c}" data-zk="c" data-v="${c}" aria-label="${c}"></button>`).join('') + `<input type="color" value="${cur || '#ff3d8b'}" data-zc="1" aria-label="Otro color">`;
  function panel() {
    const it = col[sel];
    let h = `<div class="dz-add"><button type="button" data-z="img">${ICO('image')} Foto</button><button type="button" data-z="cover">Portada</button><button type="button" data-z="txt">Aa Letras</button><button type="button" data-z="stk">★ Sticker</button><button type="button" data-z="tape">Cinta</button><button type="button" data-z="shp">● Forma</button></div>`;
    if (!it) h += '<p class="hint" style="margin:8px 2px">Toca un recorte para editarlo. Arrástralo para moverlo; con dos dedos lo agrandas y lo giras.</p>';
    else {
      h += `<label class="dz-l">Tamaño<input type="range" min="2" max="140" value="${it.w}" data-zr="w"></label><label class="dz-l">Giro<input type="range" min="-180" max="180" value="${it.r}" data-zr="r"></label><label class="dz-l">Transparencia<input type="range" min="10" max="100" value="${it.o}" data-zr="o"></label>`;
      if (it.k === 'img') h += `<div class="dz-row">${opts('cut', DZ_CUT, it.cut)}</div>`;
      if (it.k === 'txt') h += `<textarea rows="2" maxlength="60" data-zt="1">${esc(it.t)}</textarea><div class="dz-row">${opts('v', DZ_TXT, it.v)}</div>${it.v !== 'recorte' ? `<div class="dz-row">${colors(it.c)}</div>` : ''}`;
      if (it.k === 'stk') h += `<div class="dz-row">${opts('v', DZ_STK, it.v)}</div>${DZ_GLYPH[it.v] ? '' : `<textarea rows="1" maxlength="24" data-zt="1">${esc(it.t || '')}</textarea>`}<div class="dz-row">${colors(it.c)}</div>`;
      if (it.k === 'tape') h += `<div class="dz-row">${Object.keys(DZ_TAPE).map(v => `<button type="button" class="dz-chip${it.v === v ? ' on' : ''}" data-zk="v" data-v="${v}">${v[0].toUpperCase() + v.slice(1)}</button>`).join('')}</div>`;
      if (it.k === 'shp') h += `<div class="dz-row">${opts('v', DZ_SHP, it.v)}</div><div class="dz-row">${colors(it.c)}</div>`;
      h += `<div class="dz-row"><button type="button" data-z="front">Al frente</button><button type="button" data-z="back">Atrás</button><button type="button" data-z="dup">Duplicar</button><button type="button" data-z="del">Borrar</button></div>`;
    }
    bar.innerHTML = h;
  }
  const close = () => { ro.disconnect(); ed.remove(); };
  ed.addEventListener('click', async e => {
    const b = e.target.closest('button'); if (!b) return; const z = b.dataset.z, it = col[sel];
    if (z === 'cancel') return close();
    if (z === 'ok') { rev.dz.col = dzClean({ col }).col; close(); return dzChanged(true); }
    if (z === 'img') return file.click();
    if (z === 'cover') { if (!rev.cover) return toast('Primero elige una imagen de portada.'); return add({ k: 'img', s: rev.cover, w: 60, cut: 'roto' }); }
    if (z === 'txt') { const v = await uiDialog({ title: 'Letras para el collage', fields: [{ key: 't', label: 'Texto (máximo 60)' }], ok: 'Pegar' }); if (v && v.t && v.t.trim()) add({ k: 'txt', t: v.t, v: 'recorte', w: 40 }); return; }
    if (z === 'stk') return add({ k: 'stk', v: 'estallido', t: '¡Nuevo!', w: 30 });
    if (z === 'tape') return add({ k: 'tape', v: 'beige', w: 30, r: -20 });
    if (z === 'shp') return add({ k: 'shp', v: 'circulo', w: 40, c: '#ff3d8b', z: 0 });
    if (!it) return;
    if (z === 'del') { col.splice(sel, 1); sel = -1; }
    else if (z === 'dup') { if (col.length >= 30) return toast('Máximo 30 recortes.'); col.push({ ...it, x: Math.min(100, it.x + 6), y: Math.min(100, it.y + 6), z: topZ() }); sel = col.length - 1; }
    else if (z === 'front') it.z = topZ();
    else if (z === 'back') { col.forEach(o => { if (o !== it) o.z = Math.min(99, o.z + 1); }); it.z = 0; }
    else if (b.dataset.zk) { it[b.dataset.zk] = b.dataset.v; }
    else return;
    draw(); panel();
  });
  bar.addEventListener('input', e => {
    const it = col[sel]; if (!it) return;
    if (e.target.dataset.zr) { it[e.target.dataset.zr] = Number(e.target.value); draw(); return; }
    if (e.target.dataset.zt) { it.t = it.k === 'txt' ? dzCleanTxt(e.target.value) || 'Texto' : revClean(e.target.value, 24); draw(); return; }
    if (e.target.dataset.zc) { it.c = dzHex(e.target.value) || it.c; draw(); }
  });
  file.addEventListener('change', async () => {
    const fs = [...file.files]; file.value = '';
    for (const f of fs) { try { add({ k: 'img', s: (await revResize(f, 900, 0.8)).data(), w: 50, cut: 'rect' }); } catch { toast('No pude leer esa imagen.', true); } }
  });
  // Arrastrar con un dedo; agrandar y girar con dos
  const pts = new Map(); let g = null;
  cv.addEventListener('pointerdown', e => {
    const el = e.target.closest('.dz-it');
    if (!el) {
      if (pts.size === 1 && sel >= 0) { cv.setPointerCapture(e.pointerId); pts.set(e.pointerId, { x: e.clientX, y: e.clientY }); g = null; e.preventDefault(); }
      else if (!pts.size) { sel = -1; draw(); panel(); }
      return;
    }
    const i = Number(el.dataset.i); if (sel !== i) { sel = i; draw(); panel(); }
    cv.setPointerCapture(e.pointerId); pts.set(e.pointerId, { x: e.clientX, y: e.clientY }); g = null; e.preventDefault();
  });
  cv.addEventListener('pointermove', e => {
    if (!pts.has(e.pointerId) || sel < 0) return; const it = col[sel], R = cv.getBoundingClientRect(), p0 = pts.get(e.pointerId);
    if (pts.size === 1) { it.x = dzNum(it.x + (e.clientX - p0.x) / R.width * 100, -20, 120, 50); it.y = dzNum(it.y + (e.clientY - p0.y) / R.height * 100, -20, 120, 50); pts.set(e.pointerId, { x: e.clientX, y: e.clientY }); }
    else if (pts.size === 2) {
      pts.set(e.pointerId, { x: e.clientX, y: e.clientY }); const [a, b] = [...pts.values()];
      const dist = Math.hypot(b.x - a.x, b.y - a.y), ang = Math.atan2(b.y - a.y, b.x - a.x) * 180 / Math.PI;
      if (!g) g = { dist, ang, w: it.w, r: it.r }; else { it.w = dzNum(g.w * dist / Math.max(1, g.dist), 2, 140, it.w); let r = g.r + ang - g.ang; r = ((r + 540) % 360) - 180; it.r = Math.round(r); }
    }
    const el = cv.querySelector(`[data-i="${sel}"]`); if (el) el.outerHTML = dzItemHtml(it, sel).replace('class="dz-it', 'class="dz-it sel');
  });
  const up = e => { pts.delete(e.pointerId); g = null; if (!pts.size) panel(); };
  cv.addEventListener('pointerup', up); cv.addEventListener('pointercancel', up);
  const ro = new ResizeObserver(fit); ro.observe(ed.querySelector('.dz-stage'));
  draw(); panel(); fit();
}

/* --- Guardar: sube las fotos del collage y guarda el diseño en su propia columna --- */
async function dzSave(id) {
  if (!rev.dz) return; const d = rev.dz;
  for (const it of d.col) if (it.k === 'img' && it.s.startsWith('data:')) {
    toast('Subiendo recortes del collage…', false, 20000);
    const b = revDataBlob(it.s); if (!b) continue;
    it.s = await revUpload(b, 'k-' + revUid() + '.jpg');
  }
  const diff = dzDiff(d), js = JSON.stringify(diff);
  if (js === dzOrig) return;
  try { await revFetch(`/rest/v1/posts?id=eq.${id}`, { method: 'PATCH', body: { design: diff }, prefer: 'return=minimal' }); dzOrig = js; }
  catch (e) { throw new Error(/design|column|schema/i.test(e.message) ? 'El texto se guardó, pero el diseño avanzado no: falta ejecutar revista_supabase_v6.sql en Supabase.' : 'No se pudo guardar el diseño: ' + e.message); }
}

/* ===== Videos de la Revista (Bóveda, artículos): la radio se pausa mientras suena un video y vuelve sola al salir ===== */
const RV_VIDSEL = '#view-revista iframe[src*="youtube"],#view-revista iframe[src*="vimeo"],#view-revista iframe[src*="archive.org"]';
let rvHold = null;
function rvHoldTick() {
  if (document.documentElement.classList.contains('en-app-android')) return; // en la app de Android lo maneja la app
  const frames = [...document.querySelectorAll(RV_VIDSEL)];
  // Si saliste de la Revista con un video abierto, se detiene (si no, sonaría escondido encima de la radio)
  frames.filter(f => f.offsetParent === null).forEach(f => {
    const box = f.closest('#bvPlay'), v = rev.vItem;
    f.remove();
    if (box && v) box.innerHTML = `<button type="button" class="bv-poster" data-act="bvplay" aria-label="Ver el concierto">${vaultImg(v)}<span class="bv-pbtn">▶</span></button>`;
  });
  const on = frames.some(f => f.isConnected && f.offsetParent !== null);
  if (on && !rvHold) {
    let was = false;
    try { was = local() ? (!audio.paused || S.state === 'break') : !!S.playing; } catch {}
    rvHold = { was, t: 0, off: false };
    if (was) toast('Pausé la radio mientras ves el video. Vuelve sola al salir.');
  }
  if (on && rvHold && !rvHold.off) {
    try { if (speechSynthesis.speaking) { speakId++; speechSynthesis.cancel(); } } catch {}
    try { if (cloud && !cloud.paused) cloud.pause(); } catch {}
    try {
      if (local()) { if (!audio.paused) { userPaused = true; audio.pause(); } }
      else if (S.playing && Date.now() - rvHold.t > 8000) { rvHold.t = Date.now(); spot('/me/player/pause', 'PUT').then(() => { S.playing = false; try { updatePlayIcon(); } catch {} }).catch(() => {}); }
    } catch {}
  } else if (!on && rvHold) {
    const h = rvHold; rvHold = null;
    if (h.was && !h.off) try {
      if (local()) { if (audio.paused && audio.src) { userPaused = false; audio.play().catch(() => {}); } }
      else spot('/me/player/play', 'PUT').catch(() => {});
    } catch {}
  }
}
// Si tocas los controles de la radio mientras hay un video, se respeta lo que elijas
document.addEventListener('click', e => { if (rvHold && e.target.closest && e.target.closest('#pp,#carPP,#skip,#prev')) rvHold.off = true; }, true);
setInterval(rvHoldTick, 1000);
try { new MutationObserver(() => rvHoldTick()).observe($('view-revista'), { childList: true, subtree: true }); } catch {}
