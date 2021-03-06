package com.github.djaler.evilbot.components

import com.github.djaler.evilbot.clients.SentryClient
import io.sentry.Breadcrumb
import org.apache.logging.log4j.LogManager
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.CodeSignature
import org.springframework.stereotype.Component

@Aspect
@Component
class BreadcrumbAspect(
    private val sentryClient: SentryClient
) {
    companion object {
        private val log = LogManager.getLogger()
    }

    @Before("@within(RecordBreadcrumb) || @annotation(RecordBreadcrumb)")
    fun before(joinPoint: JoinPoint) {
        try {
            val signature = joinPoint.signature

            if (signature !is CodeSignature) {
                return
            }

            val methodName = "${joinPoint.target::class.java.simpleName}.${signature.name}"

            val parameterNames = signature.parameterNames
            val args: Array<Any?> = joinPoint.args
            val parametersValues = parameterNames.zip(args.map { it.toString() })

            sentryClient.addBreadcrumb(
                Breadcrumb(methodName).also { breadcrumb ->
                    parametersValues
                        .forEach { (parameter, value) -> breadcrumb.setData(parameter, value) }
                }
            )
        } catch (e: Exception) {
            log.error("Breadcrumb record error", e)

            sentryClient.captureException(e)
        }
    }
}

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RecordBreadcrumb
