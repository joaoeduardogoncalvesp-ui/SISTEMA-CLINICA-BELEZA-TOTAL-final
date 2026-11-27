package com.clinicabelezatotal.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Sobe e derruba os serviços de apoio (Redis e Evolution API) junto com o backend.
 *
 * <p>Desligado por padrão. Para usar:
 * <pre>
 *   DOCKER_LIFECYCLE=true java -jar target/clinica-beleza-total-1.0.0.jar
 * </pre>
 *
 * <p>Na inicialização, executa {@code scripts/servicos-docker.sh up} e espera a Evolution
 * API responder antes de liberar o start da aplicação. No encerramento, executa
 * {@code scripts/servicos-docker.sh down}.
 *
 * <p>Por que não desligar por padrão: no perfil de produção a aplicação pode ser
 * executada dentro do próprio Docker, onde o comando não existe; e derrubar as
 * notificações ao encerrar o backend pode surpreender quem opera a clínica.
 */
@Component
@ConditionalOnProperty(name = "docker.lifecycle.enabled", havingValue = "true")
@Slf4j
public class DockerLifecycleManager implements SmartLifecycle {

    private final boolean pararAoEncerrar;
    private final String script;
    private final String nomeScript;
    private final int timeoutSegundos;
    private volatile boolean rodando = false;

    public DockerLifecycleManager(
            @Value("${docker.lifecycle.script:scripts/servicos-docker.sh}") String script,
            @Value("${docker.lifecycle.parar-ao-encerrar:true}") boolean pararAoEncerrar,
            @Value("${docker.lifecycle.timeout-segundos:90}") int timeoutSegundos) {
        this.script = script;
        this.nomeScript = new File(script).getName();
        this.pararAoEncerrar = pararAoEncerrar;
        this.timeoutSegundos = timeoutSegundos;
    }

    /** Roda antes dos beans que dependem da Evolution API (fase alta = antes). */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }

    @Override
    public void start() {
        executar("up");
        rodando = true;
    }

    @Override
    public void stop() {
        if (rodando && pararAoEncerrar) {
            executar("down");
        }
        rodando = false;
    }

    @Override
    public boolean isRunning() {
        return rodando;
    }

    /**
     * Procura o script no diretório atual e nos diretórios superiores, para funcionar
     * tanto executando de dentro de {@code backend/} quanto da raiz do projeto.
     */
    private File localizarScript() {
        File direto = new File(script);
        if (direto.isFile()) {
            return direto;
        }
        File pasta = new File(".").getAbsoluteFile();
        while (pasta != null) {
            File candidato = new File(pasta, script);
            if (candidato.isFile()) {
                return candidato;
            }
            pasta = pasta.getParentFile();
        }
        return null;
    }

    private void executar(String acao) {
        File arquivo = localizarScript();
        if (arquivo == null) {
            log.warn("[docker-lifecycle] script '{}' não encontrado a partir de {}; ignorando ação '{}'",
                    nomeScript, new File(".").getAbsolutePath(), acao);
            return;
        }
        log.info("[docker-lifecycle] usando script: {}", arquivo.getAbsolutePath());

        List<String> comando = new ArrayList<>();
        comando.add("bash");
        comando.add(arquivo.getAbsolutePath());
        comando.add(acao);

        try {
            log.info("[docker-lifecycle] executando: {} {}", script, acao);
            ProcessBuilder pb = new ProcessBuilder(comando);
            pb.redirectErrorStream(true);
            Process processo = pb.start();

            processo.getInputStream().transferTo(System.out);

            boolean terminou = processo.waitFor(timeoutSegundos, TimeUnit.SECONDS);
            if (!terminou) {
                processo.destroyForcibly();
                log.warn("[docker-lifecycle] '{}' excedeu {}s e foi interrompido", acao, timeoutSegundos);
                return;
            }
            if (processo.exitValue() != 0) {
                log.warn("[docker-lifecycle] '{}' terminou com código {}. O sistema continua sem os serviços.",
                        acao, processo.exitValue());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[docker-lifecycle] execução de '{}' interrompida", acao);
        } catch (Exception e) {
            // Falha ao subir os containers não deve impedir o sistema de iniciar:
            // o WhatsApp é opcional e o modo mock continua disponível.
            log.warn("[docker-lifecycle] não foi possível executar '{}': {}", acao, e.getMessage());
        }
    }
}
