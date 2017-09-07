package uk.co.hello

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.Result.Failure
import com.github.kittinunf.result.Result.Success
import com.github.kittinunf.result.getAs
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.javalin.Context
import io.javalin.HaltException
import io.javalin.Javalin.start


fun main(args: Array<String>) {
    val PORT = System.getenv("port").toInt()
    val AUTH_TOKEN = System.getenv("auth_token")
    val app = start(PORT)

    // curl localhost:7000/hello?abc=43213 -H 'auth_token: abcde'
    app.get("/hello") { ctx ->
        if (ctx.reqParam("abc") == "10")
            throw Exception("some error")
        ctx.result("Hello World --> ${ctx.reqParam("abc")}")
    }

    // curl localhost:7000/http-get/dadad -H 'auth_token: abcde'
    app.get("/http-get/:id") { ctx ->
        println("This the id you provided ${ctx.param("id")}")
        "http://localhost:8500/v1/agent/services".httpGet().responseString { request, response, result ->
            //do something with response
            when (result) {
                is Result.Failure -> {
                    val error = result.getAs<String>()
                }
                is Result.Success -> {
                    val mapType = object : TypeToken<Map<String, Any>>() {}.type
                    val data: Map<String, String> = Gson().fromJson(result.get(), mapType)
                    println(data)
                }
            }
        }
    }

    app.get("/http-post") { ctx ->
        "http://localhost:8080/test/users".httpPost().header(
                "authorization" to "Basic some-token",
                "content-type" to "application/json"
        ).body("""
                {
                    "name": "Vikrant",
                    "email": "something@abc.com"
                }
        """.trimIndent()).responseString { request, response, result: Result<String, FuelError> ->
            //do something with outputResponse
            val op: Any = when (result) {
                is Failure -> {
                    val error = String(result.error.errorData)
                    error
                }
                is Success -> {
                    val mapType = object : TypeToken<Map<String, Any>>() {}.type
                    val data: Map<String, String> = Gson().fromJson(result.get(), mapType)
                    println(data)
                    data
                }
            }
            ctx.status(200).result(op.toString())
        }
    }


    // curl -X POST localhost:7000/http-post2 -d '{"message":"something to be posted on server"}' -H 'auth_token: abcde'
    app.post("/http-post2") { ctx ->
        println("This the request body you posted ${ctx.body()}")
        println("Request body as class ${jacksonObjectMapper().readValue<Test>(ctx.body(),Test::class.java)}")
        println("Request body as class ${ctx.bodyAs(Test::class.java)}")
        // following doesn't work
//        println("Request body as class ${ctx.bodyAsClass(Test::class.java)}")
        val output = "http://localhost:8080/test/users".httpPost().header(
                "authorization" to "Basic some-token",
                "content-type" to "application/json"
        ).body("""
                {
                    "name": "Vikrant",
                    "email": "something@abc.com"
                }
        """.trimIndent()).responseString { request, response, result: Result<String, FuelError> ->
            //do something with outputResponse
            val op = when (result) {
                is Failure -> {
                    val error = String(result.error.errorData)
                    error
                }
                is Success -> {
                    val mapType = object : TypeToken<Map<String, Any>>() {}.type
                    val data: Map<String, String> = Gson().fromJson(result.get(), mapType)
                    println(data)
                    data
                }
            }
            println(op)
        }
        ctx.status(200).result(String(output.response().second.data))
    }

    app.before { ctx ->
        val authToken = ctx.header("auth_token")
        println(authToken)
        if (authToken != AUTH_TOKEN) {
            throw HaltException(401, "Unauthorized access")
        }
    }

    app.exception(Exception::class.java, { e, ctx ->
        ctx.status(500).result(e.message!!)
    })
}

// extension functions
fun Context.reqParam(name: String): String = this.request().getParameter(name)
fun<T> Context.bodyAs(clazz: Class<T>): T = jacksonObjectMapper().readValue<T>(this.body(),clazz)


data class Test(val message: String)
