# Matador

Mod Fabric do lado do cliente para Minecraft 26.1.1 com um painel visual de caça a mobs.

## Requisitos

- Minecraft 26.1.1
- Fabric Loader 0.19.x
- Fabric API 0.145.1 ou mais recente para 26.1.x
- Java 25
- Gradle, ou uma IDE que consiga importar e executar projetos Gradle

## Como Usar

- Pressione `J` dentro do jogo para abrir o painel `Caçador de Mobs`.
- Selecione o mob alvo usando os cards visuais com ovos de spawn.
- Ative ou desative o caçador pelo botão `Ativar` / `Desativar`.
- Ajuste o raio de busca, a distância de ataque e o atraso entre ataques pelo painel.
- No modo `Um clique`, o mod bate uma vez no mob atual e aguarda esse mob morrer ou sumir antes de buscar o próximo.

## Compilar

```bash
gradlew.bat build
```

Se o Windows ainda estiver apontando para Java 8, use o script local configurado para o JDK instalado neste ambiente:

```bash
build-local.bat
```

O arquivo `.jar` compilado será gerado em `build/libs/`.
