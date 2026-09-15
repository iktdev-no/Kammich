package no.iktdev.kammich.system.network

import org.slf4j.LoggerFactory
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Component
class NmcliMonitor(
    private val listeners: List<NmcliMonitorListener>
) : SmartLifecycle {

    private val log = LoggerFactory.getLogger(javaClass)

    private val running = AtomicBoolean(false)

    private var process: Process? = null
    private var executor: ExecutorService? = null

    override fun start() {
        if (!running.compareAndSet(false, true)) {
            return
        }

        log.info("Starter nmcli monitor")

        executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "nmcli-monitor").apply {
                isDaemon = true
            }
        }

        executor!!.submit(::listen)
    }

    private fun listen() {
        try {
            val process = ProcessBuilder(
                "nmcli",
                "monitor"
            )
                .redirectErrorStream(true)
                .start()

            this.process = process

            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                while (running.get()) {
                    val line = reader.readLine() ?: break

                    if (line.isNotBlank()) {
                        log.debug("Dispatching NetworkManager event: {}", line)
                        listeners.forEach { it.onNetworkManagerEvent(line) }
                    }
                }
            }

            if (running.get()) {
                log.warn("nmcli monitor avsluttet uventet")
            }
        } catch (e: Exception) {
            if (running.get()) {
                log.error("Feil i nmcli monitor", e)
            }
        } finally {
            this.process = null
        }
    }

    override fun stop() {
        if (!running.compareAndSet(true, false)) {
            return
        }

        log.info("Stopper nmcli monitor")

        process?.destroy()
        process = null

        executor?.shutdownNow()
        executor = null
    }

    override fun isRunning(): Boolean = running.get()

    interface NmcliMonitorListener {
        fun onNetworkManagerEvent(event: String)
    }
}
