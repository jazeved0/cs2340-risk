package controllers.format

import controllers.InPacket
import play.api.libs.json.{JsResult, JsValue, Reads}

/**
  * Used to satisfy the compile-time macros; [unused]
  * @tparam T The type of the object that "can" be read
  */
class UnusedFormat[T <: InPacket] extends Reads[T] {
  override def reads(json: JsValue): JsResult[T] = {
    throw new NotImplementedError("Cannot deserialize internal messages")
  }
}
