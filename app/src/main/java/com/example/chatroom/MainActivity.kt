package com.example.chatroom

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.chatroom.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var socket: Socket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        binding.buttonConnect.setOnClickListener {

            Thread(Runnable {
                try {
                    val (ipAddress, port) = parseServerAddress(binding.editTextServer.text.toString())
                        ?: throw Exception("Invalid server address")
                    Log.d("MainActivity", "Connecting to $ipAddress:$port")
                    socket = Socket(ipAddress, port)

                    runOnUiThread {
                        Toast.makeText(this, "连接成功", Toast.LENGTH_SHORT).show()
                        binding.buttonConnect.isClickable = false
                        binding.buttonSend.isClickable = true
                        binding.editTextContent.toEditable()
                        binding.editTextUsername.toUneditable()
                        binding.editTextServer.toUneditable()
                    }

                } catch (e: Exception) {
                    Log.e("MainActivity", "连接失败", e)
                    runOnUiThread {
                        Toast.makeText(this, "连接失败", Toast.LENGTH_SHORT).show()
                    }
                    return@Runnable
                }

                val loginMsg = "Username: ${binding.editTextUsername.text}"
                socket!!.getOutputStream().write(loginMsg.toByteArray())

                val streamReader =
                    InputStreamReader(socket!!.getInputStream(), "utf-8")
                val reader = BufferedReader(streamReader)

                while (!isFinishing) {
                    Log.d("MainActivity", "Connected")
                    try {
                        val message = reader.readLine()
                        Log.d("MainActivity", "Received message: $message")
                        val newText = "${binding.textViewMessage.text}$message\n"
                        runOnUiThread {
                            binding.textViewMessage.text = newText
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "服务器断开链接", e)

                        binding.buttonConnect.isClickable = true
                        binding.buttonSend.isClickable = false
                        binding.editTextContent.toUneditable()
                        binding.editTextUsername.toEditable()
                        binding.editTextServer.toEditable()

                        break
                    }
                }

                socket?.close()

            }).start()
        }

        binding.buttonSend.setOnClickListener {
            Thread(Runnable {
                try {
                    val message = binding.editTextContent.text.toString()

                    if(message.isEmpty()) {
                        runOnUiThread {
                            Toast.makeText(this, "发送内容不能为空", Toast.LENGTH_SHORT).show()
                        }
                        return@Runnable
                    }

                    Log.d("MainActivity", "Sending message: $message")
                    socket?.getOutputStream()?.write(message.toByteArray())
                    runOnUiThread {
                        binding.editTextContent.text.clear()
                        val newText = "${binding.textViewMessage.text}$message\n"
                        binding.textViewMessage.text = newText
                        Toast.makeText(this, "发送成功", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "发送失败", e)
                    runOnUiThread {
                        Toast.makeText(this, "发送失败", Toast.LENGTH_SHORT).show()
                    }
                }
                inputManager.hideSoftInputFromWindow(window.decorView.windowToken, 0)
            }).start()
        }

    }

    private fun parseServerAddress(serverAddress: String): Pair<String, Int>? {
        val parts = serverAddress.split(":")

        return try {
            val host = parts[0]
            val port = parts[1].toInt()
            Pair(host, port)
        } catch (e: Exception) {
            null
        }
    }
}

fun EditText.toUneditable() {
    this.isFocusableInTouchMode = false
    this.isClickable = false
    this.isCursorVisible = false
}

fun EditText.toEditable() {
    this.isFocusableInTouchMode = true
    this.isClickable = true
    this.isCursorVisible = true
}