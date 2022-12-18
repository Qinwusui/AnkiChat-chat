package com.wusui.plugin

import com.google.gson.Gson
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.ktor.websocket.serialization.*
import kotlinx.coroutines.channels.consumeEach
import java.util.*
import kotlin.collections.LinkedHashSet

data class MsgData(
    val nickName: String,
    val msg: String,
    val chatRoomId: String,
    val qq: String
)

data class UserSession(
    val uuid: String,
    val nickName: String,
)

data class GroupList(
    val list: MutableList<Group>
)

data class Group(
    val groupName: String,
    val groupId: String
)

class Conn(val session: DefaultWebSocketSession)

/**
 * @author wusui
 *
 * WebSocket 聊天室
 */
fun Routing.anki() {
    install(Sessions) {
        cookie<UserSession>("chat") {
            cookie.path = "/"
        }
    }
    route("/anki") {
        val connections = Collections.synchronizedSet<Conn>(LinkedHashSet())
        get("/groupList") {
            call.respond(
                GroupList(
                    //TODO 可以自己添加群组，暂时不支持动态建群组
                    list = mutableListOf(
                        Group("测试群聊1", "聊天室 0"),
                        Group("测试群聊1", "聊天室 1")
                    )
                )
            )
        }
        webSocket("/chat") {
            val conn = Conn(this)
            connections.add(conn)
            try {
                incoming.consumeEach { frame ->
                    val gson = Gson()
                    val b = gson.fromJson(frame.readBytes().decodeToString(), MsgData::class.java)
                    connections.forEach {
                        it.session.sendSerializedBase(
                            MsgData(
                                b.nickName,
                                b.msg,
                                b.chatRoomId,
                                b.qq
                            ),
                            GsonWebsocketContentConverter(),
                            Charsets.UTF_8
                        )
                    }
                }
            } catch (e: Exception) {
                println(e.message)
            } finally {
                connections.remove(conn)
                close()
            }

        }
        val c=Collections.synchronizedSet(LinkedHashSet<Conn>())
        webSocket("/video") {
            val conn=Conn(this)
            c.add(conn)
            try {
                incoming.consumeEach {f->
                    c.forEach{con->
                        con.session.send(f)
                    }
                }
            }catch (e:Exception){
                println(e)
            }finally {
                c.remove(conn)
                close()
            }
        }
    }
}
