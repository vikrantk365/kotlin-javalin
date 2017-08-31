package uk.co.hello

import io.javalin.Context
import io.javalin.HaltException
import io.javalin.Javalin.start

fun main(args: Array<String>) {
    println("Hello")
    val AUTH_TOKEN = args[0]
    val app = start(7000)
    app.get("/hello") { ctx ->
        if (ctx.reqParam("abc") == "10")
            throw Exception("some error")
        ctx.result("Hello World --> ${ctx.reqParam("abc")}")
    }

    app.before { ctx ->
        val authToken = ctx.header("auth_token")
        if (authToken != AUTH_TOKEN) {
            throw HaltException(401, "Unauthorized access")
        }
    }
}

// extension function
fun Context.reqParam(name: String): String = this.request().getParameter(name)
