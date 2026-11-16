package com.fittrainer.pro.data.assessoria

import com.fittrainer.pro.data.local.JsonStore
import com.fittrainer.pro.data.local.PlanoAssessoriaEntity
import com.fittrainer.pro.data.local.TreinoEntity
import com.fittrainer.pro.data.local.WorkoutExercise
import com.fittrainer.pro.data.local.WorkoutSet
import com.fittrainer.pro.util.SafeUrl
import com.fittrainer.pro.util.Time

object AssessoriaHtml {
    fun gerar(
        plano: PlanoAssessoriaEntity,
        dias: List<DiaPlanoTreino>,
        treinos: List<TreinoEntity>,
        alunoNome: String = plano.clienteNome
    ): String {
        val sessoes = dias.sortedWith(compareBy({ it.semana }, { it.diaSemana })).map { slot ->
            val treino = treinos.find { it.id == slot.treinoId }
            val diaChave = chaveDia(slot.diaSemana)
            val diaNome = PlanosAssessoria.nomesDias.getOrNull(slot.diaSemana - 1) ?: "Dia ${slot.diaSemana}"
            SessaoHtml(
                chave = "s${slot.semana}-$diaChave",
                chaveDia = diaChave,
                diaNome = diaNome,
                semana = slot.semana,
                titulo = treino?.titulo ?: "Treino",
                duracao = treino?.duracaoEstimada,
                sets = treino?.let { JsonStore.decodeSets(it.exerciciosJson) }.orEmpty()
            )
        }
        return pagina(
            titulo = plano.nome,
            aluno = alunoNome,
            subtitulo = "${sessoes.size} dia(s) de treino",
            arquivo = nomeArquivo(plano),
            sessoes = sessoes
        )
    }

    fun gerarTreino(treino: TreinoEntity, alunoNome: String): String {
        val sets = JsonStore.decodeSets(treino.exerciciosJson)
        return pagina(
            titulo = treino.titulo,
            aluno = alunoNome,
            subtitulo = "${treino.duracaoEstimada} min",
            arquivo = nomeArquivoTreino(treino),
            sessoes = listOf(
                SessaoHtml(
                    chave = "treino",
                    chaveDia = "treino",
                    diaNome = "Treino",
                    semana = null,
                    titulo = treino.titulo,
                    duracao = treino.duracaoEstimada,
                    sets = sets
                )
            )
        )
    }

    fun nomeArquivo(plano: PlanoAssessoriaEntity): String = slug("assessoria", plano.nome)

    fun nomeArquivoTreino(treino: TreinoEntity): String = slug("treino", treino.titulo)

    private data class SessaoHtml(
        val chave: String,
        val chaveDia: String,
        val diaNome: String,
        val semana: Int?,
        val titulo: String,
        val duracao: Int?,
        val sets: List<WorkoutSet>
    )

    private fun pagina(
        titulo: String,
        aluno: String,
        subtitulo: String,
        arquivo: String,
        sessoes: List<SessaoHtml>
    ): String {
        val semanas = sessoes.mapNotNull { it.semana }.distinct().sorted()
        val dias = sessoes.filter { it.chaveDia != "treino" }.distinctBy { it.chaveDia }
        val temSemana = semanas.size > 1
        val temDia = dias.size > 1
        val chaveLocal = slug("ftp", "$titulo-$aluno").removeSuffix(".html")

        val radios = buildString {
            if (temSemana) {
                append("""<input class="filtro" type="radio" name="semana" id="sem-todos" checked>""")
                semanas.forEach { n ->
                    append("""<input class="filtro" type="radio" name="semana" id="sem-$n">""")
                }
            }
            if (temDia) {
                append("""<input class="filtro" type="radio" name="dia" id="dia-todos" checked>""")
                dias.forEach { sessao ->
                    append("""<input class="filtro" type="radio" name="dia" id="dia-${esc(sessao.chaveDia)}">""")
                }
            }
        }
        val regrasFiltro = buildString {
            if (temSemana) {
                semanas.forEach { n ->
                    append("#sem-$n:checked ~ main .sessao:not(.sem-$n){display:none;}")
                    append("#sem-$n:checked ~ .filtros-semana label[for=\"sem-$n\"]{background:#216d4d;color:#fff;}")
                }
                append("#sem-todos:checked ~ .filtros-semana label[for=\"sem-todos\"]{background:#216d4d;color:#fff;}")
            }
            if (temDia) {
                dias.forEach { sessao ->
                    append("#dia-${sessao.chaveDia}:checked ~ main .sessao:not(.dia-${sessao.chaveDia}){display:none;}")
                    append("#dia-${sessao.chaveDia}:checked ~ .filtros-dia label[for=\"dia-${sessao.chaveDia}\"]{background:#216d4d;color:#fff;}")
                }
                append("#dia-todos:checked ~ .filtros-dia label[for=\"dia-todos\"]{background:#216d4d;color:#fff;}")
            }
        }
        val navSemana = if (temSemana) {
            buildString {
                append("""<nav class="filtros filtros-semana" aria-label="Filtrar por semana"><span class="filtro-legenda">Semana</span>""")
                append("""<label class="chip" for="sem-todos">Todas</label>""")
                semanas.forEach { n ->
                    append("""<label class="chip" for="sem-$n">Semana $n</label>""")
                }
                append("</nav>")
            }
        } else {
            ""
        }
        val navDia = if (temDia) {
            buildString {
                append("""<nav class="filtros filtros-dia${if (temSemana) " deslocado" else ""}" aria-label="Filtrar por dia"><span class="filtro-legenda">Dia</span>""")
                append("""<label class="chip" for="dia-todos">Todos</label>""")
                dias.forEach { sessao ->
                    append("""<label class="chip" for="dia-${esc(sessao.chaveDia)}">${esc(sessao.diaNome)}</label>""")
                }
                append("</nav>")
            }
        } else {
            ""
        }
        val dicaFiltro = when {
            temSemana && temDia -> " · filtre por semana e por dia"
            temSemana -> " · filtre pela semana"
            temDia -> " · toque no dia para filtrar"
            else -> ""
        }
        val blocos = sessoes.joinToString("\n") { sessao ->
            val classeSemana = sessao.semana?.let { " sem-$it" }.orEmpty()
            val exerciciosHtml = if (sessao.sets.all { it.exercicios.isEmpty() }) {
                """<p class="vazio">Nenhum exercício neste dia.</p>"""
            } else {
                var ordem = 0
                sessao.sets.mapIndexed { setIndex, set ->
                    val itens = set.exercicios.map { ex ->
                        ordem += 1
                        exercicio(ordem, ex, "e-${sessao.chave}-$ordem")
                    }.joinToString("\n")
                    val descanso = if (set.descansoSegundos > 0) {
                        """<p class="set-rest">Descanso após o SET: ${esc(Time.formatMmSs(set.descansoSegundos))}</p>"""
                    } else {
                        ""
                    }
                    """
                    <section class="set">
                      <p class="set-cab">${esc(set.titulo(setIndex))}</p>
                      $itens
                      $descanso
                    </section>
                    """.trimIndent()
                }.joinToString("\n")
            }
            val meta = listOfNotNull(
                sessao.semana?.let { "Semana $it" },
                sessao.duracao?.let { "$it min" }
            ).joinToString(" · ")
            """
            <article class="sessao dia-${esc(sessao.chaveDia)}$classeSemana" data-sessao="${esc(sessao.chave)}">
              <header class="sessao-cab">
                <p class="dia">${esc(sessao.diaNome)}</p>
                <h2>${esc(sessao.titulo)}</h2>
                ${if (meta.isNotBlank()) """<p class="meta">${esc(meta)}</p>""" else ""}
              </header>
              $exerciciosHtml
              <label class="feito">
                <input type="checkbox" data-check="t-${esc(sessao.chave)}">
                <span>Treino realizado</span>
              </label>
            </article>
            """.trimIndent()
        }
        return """
<!DOCTYPE html>
<html lang="pt-BR" data-chave="${esc(chaveLocal)}" data-arquivo="${esc(arquivo)}">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1"/>
<title>${esc(titulo)}</title>
<style>
  :root { color-scheme: light; }
  * { box-sizing: border-box; }
  body {
    margin: 0;
    background: #f3f6f4;
    color: #12241c;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
    font-size: 18px;
    line-height: 1.4;
  }
  .topo {
    background: #216d4d;
    color: #fff;
    padding: 20px 18px 16px;
  }
  .marca { margin: 0; font-size: 13px; letter-spacing: .12em; text-transform: uppercase; opacity: .85; font-weight: 700; }
  h1 { margin: 8px 0 4px; font-size: 28px; line-height: 1.15; }
  .aluno { margin: 0; font-size: 18px; font-weight: 600; }
  .sub { margin: 6px 0 0; font-size: 15px; opacity: .9; }
  .acoes {
    display: flex; gap: 8px; flex-wrap: wrap; align-items: center;
    padding: 12px 16px; background: #14553c; color: #fff;
  }
  .acoes button, .acoes .status {
    border: 0; border-radius: 999px; padding: 10px 16px;
    font-size: 15px; font-weight: 800; cursor: pointer;
  }
  .acoes button { background: #fff; color: #14553c; }
  .acoes .status { background: transparent; color: #d9f0e4; padding-left: 0; }
  .filtros {
    display: flex; gap: 8px; flex-wrap: wrap; align-items: center;
    padding: 12px 16px;
    background: #fff;
    border-bottom: 1px solid #d7e3dc;
    position: sticky; top: 0; z-index: 1;
  }
  .filtros-dia.deslocado { top: 58px; }
  .filtro-legenda {
    font-size: 12px; font-weight: 800; letter-spacing: .08em;
    text-transform: uppercase; color: #5b6f66; margin-right: 4px;
  }
  .chip {
    border: 0; border-radius: 999px;
    padding: 10px 16px;
    font-size: 16px; font-weight: 700;
    background: #e7f0eb; color: #216d4d;
    cursor: pointer;
  }
  main { padding: 16px; max-width: 720px; margin: 0 auto; }
  .sessao {
    background: #fff;
    border-radius: 18px;
    padding: 18px;
    margin-bottom: 16px;
    box-shadow: 0 1px 0 #d7e3dc;
  }
  .sessao.ok { box-shadow: inset 0 0 0 3px #216d4d; }
  .dia {
    display: inline-block; margin: 0 0 8px;
    background: #d9f0e4; color: #14553c;
    font-size: 14px; font-weight: 800; letter-spacing: .04em;
    text-transform: uppercase; padding: 6px 10px; border-radius: 8px;
  }
  .sessao-cab h2 { margin: 0; font-size: 24px; }
  .meta { margin: 6px 0 0; color: #5b6f66; font-size: 15px; }
  .set { margin-top: 8px; padding-top: 8px; border-top: 1px solid #e6eee9; }
  .set-cab {
    margin: 0 0 4px; font-size: 13px; font-weight: 800;
    letter-spacing: .08em; text-transform: uppercase; color: #14553c;
  }
  .set-rest {
    margin: 8px 0 0; padding: 12px 14px; border-radius: 12px;
    background: #d9f0e4; color: #14553c; font-size: 16px; font-weight: 800;
  }
  .ex {
    display: grid;
    grid-template-columns: 44px 1fr;
    gap: 10px 12px;
    padding: 16px 0;
    border-top: 1px solid #e6eee9;
  }
  .ex.ok { opacity: .7; }
  .num {
    width: 44px; height: 44px; border-radius: 12px;
    background: #216d4d; color: #fff;
    display: flex; align-items: center; justify-content: center;
    font-size: 20px; font-weight: 800;
  }
  .nome { margin: 0; font-size: 22px; font-weight: 800; line-height: 1.2; }
  .grupo { margin: 4px 0 0; color: #5b6f66; font-size: 15px; font-weight: 600; }
  .stats {
    grid-column: 1 / -1;
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }
  .stat {
    background: #f3f8f5;
    border-radius: 12px;
    padding: 10px 8px;
    text-align: center;
  }
  .stat b { display: block; font-size: 26px; line-height: 1.1; color: #14553c; }
  .stat span { display: block; margin-top: 4px; font-size: 12px; font-weight: 700; letter-spacing: .06em; text-transform: uppercase; color: #5b6f66; }
  .obs { grid-column: 1 / -1; margin: 0; color: #3d5249; font-size: 16px; }
  .video { grid-column: 1 / -1; font-size: 16px; font-weight: 700; color: #216d4d; }
  .feito, .feito-ex {
    display: flex; gap: 10px; align-items: center;
    font-weight: 800; cursor: pointer;
  }
  .feito {
    margin-top: 12px; padding: 14px;
    background: #e7f0eb; border-radius: 14px; font-size: 18px;
  }
  .feito-ex { grid-column: 1 / -1; font-size: 16px; color: #14553c; }
  .feito input, .feito-ex input { width: 24px; height: 24px; accent-color: #216d4d; }
  .vazio { color: #5b6f66; }
  .filtro { position: absolute; opacity: 0; width: 1px; height: 1px; }
  $regrasFiltro
</style>
</head>
<body>
  <header class="topo">
    <p class="marca">FitTrainer Pro</p>
    <h1>${esc(titulo)}</h1>
    ${if (aluno.isNotBlank()) """<p class="aluno">${esc(aluno)}</p>""" else ""}
    <p class="sub">${esc(subtitulo)}$dicaFiltro</p>
  </header>
  <div class="acoes">
    <button type="button" id="btn-salvar">Salvar HTML neste aparelho</button>
    <span class="status" id="status-save">As marcas ficam salvas ao fechar e abrir este arquivo.</span>
  </div>
  $radios
  $navSemana
  $navDia
  <main>
    $blocos
  </main>
  <script type="application/json" id="progresso">{}</script>
  <script>
    (function () {
      var raiz = document.documentElement;
      var chave = "ftp-html-" + (raiz.getAttribute("data-chave") || "treino");
      var arquivo = raiz.getAttribute("data-arquivo") || "treino.html";
      var status = document.getElementById("status-save");
      var jsonEl = document.getElementById("progresso");
      var handleArquivo = null;
      var gravando = false;
      function caixas() { return document.querySelectorAll("[data-check]"); }
      function ler() {
        var o = {};
        caixas().forEach(function (el) { o[el.getAttribute("data-check")] = !!el.checked; });
        return o;
      }
      function aplicar(estado) {
        caixas().forEach(function (el) {
          var id = el.getAttribute("data-check");
          if (estado && Object.prototype.hasOwnProperty.call(estado, id)) el.checked = !!estado[id];
          sincronizar(el);
        });
      }
      function sincronizar(el) {
        if (el.checked) el.setAttribute("checked", "checked");
        else el.removeAttribute("checked");
        var artigo = el.closest(".sessao");
        var linha = el.closest(".ex");
        if (el.getAttribute("data-check").indexOf("t-") === 0 && artigo) artigo.classList.toggle("ok", el.checked);
        if (linha) linha.classList.toggle("ok", el.checked);
      }
      function abrirDb() {
        return new Promise(function (resolve, reject) {
          if (!window.indexedDB) { reject(); return; }
          var req = indexedDB.open("fittrainer-pro-html", 2);
          req.onupgradeneeded = function () {
            var db = req.result;
            if (!db.objectStoreNames.contains("progresso")) db.createObjectStore("progresso");
            if (!db.objectStoreNames.contains("arquivos")) db.createObjectStore("arquivos");
          };
          req.onsuccess = function () { resolve(req.result); };
          req.onerror = function () { reject(req.error); };
        });
      }
      function idbLer() {
        return abrirDb().then(function (db) {
          return new Promise(function (resolve) {
            var r = db.transaction("progresso").objectStore("progresso").get(chave);
            r.onsuccess = function () { resolve(r.result || {}); };
            r.onerror = function () { resolve({}); };
          });
        }).catch(function () { return {}; });
      }
      function idbGravar(o) {
        return abrirDb().then(function (db) {
          return new Promise(function (resolve) {
            var r = db.transaction("progresso", "readwrite").objectStore("progresso").put(o, chave);
            r.onsuccess = function () { resolve(); };
            r.onerror = function () { resolve(); };
          });
        }).catch(function () {});
      }
      function handleLer() {
        return abrirDb().then(function (db) {
          return new Promise(function (resolve) {
            var r = db.transaction("arquivos").objectStore("arquivos").get(chave);
            r.onsuccess = function () { resolve(r.result || null); };
            r.onerror = function () { resolve(null); };
          });
        }).catch(function () { return null; });
      }
      function handleGravar(h) {
        return abrirDb().then(function (db) {
          return new Promise(function (resolve) {
            var r = db.transaction("arquivos", "readwrite").objectStore("arquivos").put(h, chave);
            r.onsuccess = function () { resolve(); };
            r.onerror = function () { resolve(); };
          });
        }).catch(function () {});
      }
      function htmlAtual() {
        caixas().forEach(sincronizar);
        jsonEl.textContent = JSON.stringify(ler());
        return "<!DOCTYPE html>\\n" + raiz.outerHTML;
      }
      function baixar() {
        var blob = new Blob([htmlAtual()], { type: "text/html;charset=utf-8" });
        var a = document.createElement("a");
        a.href = URL.createObjectURL(blob);
        a.download = arquivo;
        document.body.appendChild(a);
        a.click();
        a.remove();
        setTimeout(function () { URL.revokeObjectURL(a.href); }, 1500);
      }
      function escreverHandle(h) {
        if (!h || gravando) return Promise.resolve(false);
        gravando = true;
        return h.createWritable().then(function (w) {
          return w.write(htmlAtual()).then(function () { return w.close(); });
        }).then(function () {
          gravando = false;
          return true;
        }).catch(function () {
          gravando = false;
          handleArquivo = null;
          return false;
        });
      }
      function persistir(baixarCopia) {
        var o = ler();
        caixas().forEach(sincronizar);
        jsonEl.textContent = JSON.stringify(o);
        try { localStorage.setItem(chave, JSON.stringify(o)); } catch (e) {}
        try { sessionStorage.setItem(chave, JSON.stringify(o)); } catch (e) {}
        idbGravar(o);
        if (status) status.textContent = "Progresso salvo. Pode fechar e abrir de novo.";
        if (handleArquivo) {
          escreverHandle(handleArquivo).then(function (ok) {
            if (ok && status) status.textContent = "Progresso gravado neste arquivo HTML.";
          });
          return;
        }
        if (baixarCopia) baixar();
      }
      async function escolherArquivo() {
        persistir(false);
        if (window.showSaveFilePicker) {
          try {
            handleArquivo = await window.showSaveFilePicker({
              suggestedName: arquivo,
              types: [{ description: "HTML", accept: { "text/html": [".html"] } }]
            });
            await handleGravar(handleArquivo);
            var ok = await escreverHandle(handleArquivo);
            if (ok && status) status.textContent = "Este arquivo HTML foi vinculado. As marcas persistem ao reabrir.";
            return;
          } catch (e) {
            if (e && e.name === "AbortError") return;
          }
        }
        baixar();
        if (status) status.textContent = "Cópia salva em Downloads. Abra sempre esse HTML para ver as marcas.";
      }
      function mesclar(destino, origem) {
        if (!origem) return destino;
        Object.keys(origem).forEach(function (k) { destino[k] = origem[k]; });
        return destino;
      }
      var estado = {};
      try { estado = mesclar(estado, JSON.parse(jsonEl.textContent || "{}")); } catch (e) {}
      try { estado = mesclar(estado, JSON.parse(localStorage.getItem(chave) || "{}")); } catch (e) {}
      try { estado = mesclar(estado, JSON.parse(sessionStorage.getItem(chave) || "{}")); } catch (e) {}
      aplicar(estado);
      idbLer().then(function (idb) {
        estado = mesclar(estado, idb);
        aplicar(estado);
        persistir(false);
      });
      handleLer().then(function (h) {
        if (!h) return;
        handleArquivo = h;
        if (h.queryPermission) {
          h.queryPermission({ mode: "readwrite" }).then(function (p) {
            if (p === "granted") escreverHandle(h);
            else if (p === "prompt" && h.requestPermission) h.requestPermission({ mode: "readwrite" });
          });
        }
      });
      if (navigator.storage && navigator.storage.persist) navigator.storage.persist();
      caixas().forEach(function (el) {
        el.addEventListener("change", function () { persistir(false); });
      });
      document.getElementById("btn-salvar").addEventListener("click", function () { escolherArquivo(); });
      document.addEventListener("visibilitychange", function () {
        if (document.visibilityState === "hidden") persistir(false);
      });
      window.addEventListener("pagehide", function () { persistir(false); });
    })();
  </script>
</body>
</html>
        """.trimIndent()
    }

    private fun exercicio(ordem: Int, ex: WorkoutExercise, checkId: String): String {
        val carga = if (ex.tipoExecucao == "tempo") {
            ex.tempoExecucao?.let { Time.formatMmSs(it) }.orEmpty().ifBlank { "—" }
        } else {
            ex.repeticoes.ifBlank { "—" }
        }
        val cargaLabel = if (ex.tipoExecucao == "tempo") "Tempo" else "Reps"
        val video = SafeUrl.httpOrHttps(ex.videoPrincipal.ifBlank { ex.videoAlternativo })?.toString().orEmpty()
        return """
        <div class="ex">
          <div class="num">$ordem</div>
          <div>
            <p class="nome">${esc(ex.nome.ifBlank { "Exercício $ordem" })}</p>
            ${if (ex.grupoMuscular.isNotBlank()) """<p class="grupo">${esc(ex.grupoMuscular)}</p>""" else ""}
          </div>
          <div class="stats">
            <div class="stat"><b>${esc(ex.series.toString())}</b><span>Séries</span></div>
            <div class="stat"><b>${esc(carga)}</b><span>$cargaLabel</span></div>
          </div>
          ${if (ex.observacoes.isNotBlank()) """<p class="obs">${esc(ex.observacoes)}</p>""" else ""}
          ${if (video.isNotBlank()) """<a class="video" href="${esc(video)}">Ver vídeo</a>""" else ""}
          <label class="feito-ex">
            <input type="checkbox" data-check="${esc(checkId)}">
            <span>Exercício feito</span>
          </label>
        </div>
        """.trimIndent()
    }

    private fun chaveDia(diaSemana: Int): String = when (diaSemana) {
        1 -> "segunda"
        2 -> "terca"
        3 -> "quarta"
        4 -> "quinta"
        5 -> "sexta"
        6 -> "sabado"
        else -> "domingo"
    }

    private fun slug(prefixo: String, nome: String): String {
        val parte = nome.lowercase()
            .replace("[^a-z0-9]+".toRegex(), "-")
            .trim('-')
            .ifBlank { "treino" }
            .take(40)
        return "$prefixo-$parte.html"
    }

    private fun esc(value: String): String = buildString(value.length) {
        value.forEach { ch ->
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(ch)
            }
        }
    }
}
