package com.zfr.grpc

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.okhttp.OkHttpChannelBuilder
import io.grpc.stub.StreamObserver


import my.service.timeservice.Time
import my.service.timeservice.TimeServiceGrpc

import java.io.Closeable
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.security.cert.CertificateException


class TimeClient(private val channel: ManagedChannel) : Closeable {

    private val DOMAIN = "example.com"

    private val stub = TimeServiceGrpc.newStub(channel)

    suspend fun time() {
        val request = Time.GetTimeRequest.newBuilder().build()
        val obs = object : StreamObserver<Time.GetTimeResponse> {
            override fun onNext(value: Time.GetTimeResponse?) {
                println("GOOOD RESPONSE Y")
                println(value?.time)
            }

            override fun onError(t: Throwable?) {
                println("Something wrong :(")
                println("ERROR RESPONSE")
                println(t?.localizedMessage)
            }

            override fun onCompleted() {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
                println("COMPLETED RESPONSE")
            }
        }

        stub.getTime(request, obs)

    }

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }

}


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sendReq()
    }

    fun secureChannel() : ManagedChannel {
        val channelSecureTls = ManagedChannelBuilder
            .forAddress(DOMAIN, 443)
            .useTransportSecurity()
            .build()

        return channelSecureTls
    }

    fun insecureChannel() : ManagedChannel {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            @Throws(CertificateException::class)
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
                println("1")
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
                println("2")
            }

            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                println("3")
                return arrayOf()
            }
        })
        val hostnameVerifier = HostnameVerifier{_, _ -> true}
        val sslContext = SSLContext.getInstance("TLSv1.2")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        val channelInsecureTls = OkHttpChannelBuilder
            .forAddress(DOMAIN, 443)
            //.useTransportSecurity()
            .sslSocketFactory(sslContext.socketFactory)
            .hostnameVerifier(hostnameVerifier)
            .build()

        return channelInsecureTls
    }

    fun sendReq() = runBlocking {
        try {
            val channel = secureChannel()
            // val channel = insecureChannel()
            val client = TimeClient(channel)
            client.time()
            println("GOOOD 4")
        } catch (e: Exception) {
            println("OBOSRRATUSHKI")
        }
    }
}