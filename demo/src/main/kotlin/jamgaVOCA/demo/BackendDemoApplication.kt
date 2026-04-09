package jamgaVOCA.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@EnableAsync
@SpringBootApplication
class BackendDemoApplication

fun main(args: Array<String>) {
	runApplication<BackendDemoApplication>(*args)
}
