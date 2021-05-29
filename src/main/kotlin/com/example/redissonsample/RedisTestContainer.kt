package com.example.redissonsample

import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

class RedisTestContainer(imageName: DockerImageName): GenericContainer<RedisTestContainer>(imageName) {
}