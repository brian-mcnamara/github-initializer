package dev.bmac.github

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.servlet.config.annotation.EnableWebMvc

@SpringBootApplication
@EnableWebMvc
class GitHubInitializer

fun main(args: Array<String>) {
    runApplication<GitHubInitializer>(*args)
}
