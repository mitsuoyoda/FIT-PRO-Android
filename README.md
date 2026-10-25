# FitTrainer Pro (Android)

App nativo offline para personal trainers. Alunos, treinos, aulas, avaliações, progresso e financeiro ficam no **SQLite do aparelho**. Não há servidor.

## O que já funciona

- Início, agenda, alunos, treinos e menu Mais
- Criar, editar e excluir registros localmente
- Check-in de aula (gera lançamento financeiro)
- Backup em **Mais → Backup**: exportar JSON, importar mesclando ou substituindo tudo

## Como abrir

1. Instale o [Android Studio](https://developer.android.com/studio) (inclui JDK e Android SDK).
2. **File → Open** e escolha esta pasta: `~/Projects/fittrainer-pro`
3. Deixe o Gradle sincronizar.
4. Rode em um emulador ou celular (Run).

O banco local é o arquivo `fittrainer.db` no armazenamento interno do app. Desinstalar o app apaga os dados — exporte um backup antes.

## Backup

O arquivo exportado é um JSON com o formato `fittrainer-pro-backup` (versão 1). Guarde no Drive, Files ou outro lugar seguro. Na importação:

- **Mesclar** adiciona/atualiza pelo `id`
- **Substituir** apaga o banco atual e restaura o arquivo
