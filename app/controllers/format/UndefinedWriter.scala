package controllers.format

import play.api.libs.json.{JsValue, Writes}

/**
  * Used to satisfy the compile-time macros; [unused]
  * @tparam T The type of object that this "can" write
  */
class UndefinedWriter[T](typeName: String) extends Writes[T] {
  override def writes(t: T): JsValue = {
    throw new NotImplementedError(s"Cannot serialize $typeName's")
  }
}
