package com.banco.fidc.auth.repository.config

import com.banco.fidc.auth.external.aws.service.AwsSecretManagerService
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import java.util.*
import jakarta.persistence.EntityManagerFactory
import javax.sql.DataSource

@Configuration
class PostgresConfig(
    private val awsSecretManagerService: AwsSecretManagerService,
    @Value("\${database.secret.name}") private val databaseSecretName: String,
    @Value("\${spring.jpa.show-sql:false}") private val showSql: Boolean,
    @Value("\${spring.jpa.properties.hibernate.format_sql:false}") private val formatSql: Boolean,
    @Value("\${spring.jpa.generate-ddl:false}") private val generateDdl: Boolean,
    @Value("\${spring.jpa.hibernate.ddl-auto:validate}") private val ddlAuto: String
) {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    data class DatabaseConfig(
        val username: String,
        val password: String,
        val host: String,
        val port: Int,
        val dbname: String
    )

    @Bean
    @Primary
    fun dataSource(): DataSource {
        logger.info("Configurando DataSource com secret: $databaseSecretName")
        
        val databaseConfig = getDatabaseConfig()
        
        val hikariConfig = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = "jdbc:postgresql://${databaseConfig.host}:${databaseConfig.port}/${databaseConfig.dbname}"
            username = databaseConfig.username
            password = databaseConfig.password
            
            maximumPoolSize = 10
            minimumIdle = 2
            connectionTimeout = 30000
            idleTimeout = 600000
            maxLifetime = 1800000
            
            connectionTestQuery = "SELECT 1"
            isAutoCommit = false
            
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            addDataSourceProperty("useServerPrepStmts", "true")
        }
        
        logger.info("DataSource configurado com sucesso para host: ${databaseConfig.host}:${databaseConfig.port}")
        return HikariDataSource(hikariConfig)
    }

    @Bean
    @Primary
    fun entityManagerFactory(dataSource: DataSource): LocalContainerEntityManagerFactoryBean {
        val entityManagerFactoryBean = LocalContainerEntityManagerFactoryBean()
        entityManagerFactoryBean.dataSource = dataSource
        entityManagerFactoryBean.setPackagesToScan("com.banco.fidc.auth.repository.jpa")
        
        val vendorAdapter = HibernateJpaVendorAdapter()
        vendorAdapter.setShowSql(showSql)
        vendorAdapter.setGenerateDdl(generateDdl)
        entityManagerFactoryBean.jpaVendorAdapter = vendorAdapter
        
        val properties = Properties().apply {
            setProperty("hibernate.hbm2ddl.auto", ddlAuto)
            setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
            setProperty("hibernate.format_sql", formatSql.toString())
            setProperty("hibernate.use_sql_comments", formatSql.toString())
            setProperty("hibernate.jdbc.batch_size", "25")
            setProperty("hibernate.order_inserts", "true")
            setProperty("hibernate.order_updates", "true")
            setProperty("hibernate.jdbc.batch_versioned_data", "true")
        }
        
        entityManagerFactoryBean.setJpaProperties(properties)
        return entityManagerFactoryBean
    }

    @Bean
    @Primary
    fun transactionManager(entityManagerFactory: EntityManagerFactory): PlatformTransactionManager {
        return JpaTransactionManager(entityManagerFactory)
    }
    
    private fun getDatabaseConfig(): DatabaseConfig {
        val secretMap = awsSecretManagerService.getSecretAsMap(databaseSecretName)
            ?: throw IllegalStateException("Não foi possível buscar as configurações do banco de dados na secret: $databaseSecretName")
        
        return try {
            DatabaseConfig(
                username = secretMap["username"] as? String 
                    ?: throw IllegalArgumentException("Campo 'username' não encontrado na secret"),
                password = secretMap["password"] as? String 
                    ?: throw IllegalArgumentException("Campo 'password' não encontrado na secret"),
                host = secretMap["host"] as? String 
                    ?: throw IllegalArgumentException("Campo 'host' não encontrado na secret"),
                port = when (val port = secretMap["port"]) {
                    is Int -> port
                    is String -> port.toIntOrNull() ?: throw IllegalArgumentException("Campo 'port' deve ser um número válido")
                    else -> throw IllegalArgumentException("Campo 'port' não encontrado ou inválido na secret")
                },
                dbname = secretMap["dbname"] as? String 
                    ?: throw IllegalArgumentException("Campo 'dbname' não encontrado na secret")
            )
        } catch (e: Exception) {
            logger.error("Erro ao processar configurações do banco de dados da secret: ${e.message}", e)
            throw IllegalStateException("Erro ao configurar banco de dados a partir da secret", e)
        }
    }
}