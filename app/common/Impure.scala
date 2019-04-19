package common

/**
  * Marks a function as being impure (has side effects / is non deterministic)
  */
class Impure extends scala.annotation.StaticAnnotation

object Impure {
  class Nondeterministic extends scala.annotation.StaticAnnotation
  class SideEffects extends scala.annotation.StaticAnnotation
}
