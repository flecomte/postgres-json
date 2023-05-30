package fr.postgresjson.definition

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class FunctionTest: FreeSpec({
    "Function name" - {
        "all in lower" {
            Function(
                // language=PostgreSQL
                """
                create or replace function myfun() returns text language plpgsql as
                $$ begin; end$$;
                """.trimIndent()
            ).apply {
                name shouldBe "myfun"
            }
        }

        "first letter caps" {
            Function(
                // language=PostgreSQL
                """
                create or replace function Myfun() returns text language plpgsql as
                $$ begin; end$$;
                """.trimIndent()
            ).apply {
                name shouldBe "Myfun"
            }
        }

        "with numbers" {
            Function(
                // language=PostgreSQL
                """
                create or replace function myfun001() returns text language plpgsql as
                $$ begin; end$$;
                """.trimIndent()
            ).apply {
                name shouldBe "myfun001"
            }
        }

        "escaped name with space" {
            Function(
                // language=PostgreSQL
                """
                create or replace function "My fun"() returns text language plpgsql as
                $$ begin; end$$;
                """.trimIndent()
            ).apply {
                name shouldBe "My fun"
            }
        }

        "escaped name with double quote in name" {
            Function(
                // language=PostgreSQL
                """
                create or replace function "My""fun" () returns text language plpgsql as
                $$ begin; end$$;
                """.trimIndent()
            ).apply {
                name shouldBe "My\"fun"
            }
        }

        "name with new line before and after" {
            Function(
                // language=PostgreSQL
                """
                create or replace function 
                myfun
                () 
                returns text language plpgsql as
                $$ begin; end$$;
                """.trimIndent()
            ).apply {
                name shouldBe "myfun"
            }
        }
    }


    "Parameters" - {
        "One parameter text" - {
            val param = Function(
                // language=PostgreSQL
                """
                create or replace function myfun(one text) returns text language plpgsql as
                $$ begin end;$$;
                """.trimIndent()
            ).parameters

            "should have one parameter" {
                param shouldHaveSize 1
            }

            "should have first parameter name" {
                param[0].name shouldBe "one"
            }

            "should have first parameter type name" {
                param[0].type.name shouldBe "text"
            }
        }

        "Two parameters" - {
            val param = Function(
                // language=PostgreSQL
                """
                create or replace function myfun(one text, two int) returns text language plpgsql as
                $$ begin end;$$;
                """.trimIndent()
            ).parameters

            "should have 2 parameters" {
                param shouldHaveSize 2
            }

            "should have names" {
                param[0].name shouldBe "one"
                param[1].name shouldBe "two"
            }

            "should have first parameter type name" {
                param[0].type.name shouldBe "text"
                param[1].type.name shouldBe "int"
            }
        }
    }

//    "function returns" - {
//        "should return the type text if function return text" {
//            Function(
//                // language=PostgreSQL
//                """
//                create or replace function test001() returns text language plpgsql as
//                $$ begin; end$$;
//                """.trimIndent()
//            ).returns shouldBe "text"
//        }
//
//        "return null if function return void" {
//            Function(
//                // language=PostgreSQL
//                """
//                create or replace function test001() returns void language plpgsql as
//                $$ begin; end$$;
//                """.trimIndent()
//            ).returns shouldBe null
//        }
//    }
//
//    "Parameters" - {
//        "One parameter text" - {
//            val param = Function(
//                // language=PostgreSQL
//                """
//                create or replace function myfun(
//                    one text
//                ) returns text language plpgsql as
//                $$ begin end;$$;
//                """.trimIndent()
//            ).parameters
//
//            "Function must have one parameter" {
//                param shouldHaveSize 1
//            }
//
//            "The parameter must be in lower case" {
//                param.getOrNull(0)?.name shouldBe "one"
//            }
//        }
//    }
//
//    "parameters" - {
//        val param = Function(
//            // language=PostgreSQL
//            """
//            create or replace function myfun(
//                one text,
//                "Two" INTEGER default 5,
//                "Three ""and"" half" character varying = 'Yes',
//                Three_and_more character varying(255) default 'Hello',
//                dot point default '(1, 2)'::point,
//                num NUMERIC(10, 3) default 123.654,
//                arr01 text[] default '{hello, world, "and others", and\ more, "with \", ], [ , ) and as $$ in text"}'::text[],
//                arr02 "point"[] default array['(1, 2)'::point, '(7, 12)'::point]::point[],
//                arr03 text[] default array[
//                    'text01',
//                    'text02"([,#-',
//                    null
//                    ],
//                last "text" default 'Hi'
//            ) returns text language plpgsql as
//            $$ begin end;$$;
//            """.trimIndent()
//        ).parameters
//
//        "count must be correct" {
//            param shouldHaveSize 10
//        }
//
//        "name" - {
//            "in lower case" {
//                param.getOrNull(0)?.name shouldBe "one"
//            }
//            "in camel case with double quote" {
//                param.getOrNull(1)?.name shouldBe "Two"
//            }
//            "with spaces and double quote" {
//                param.getOrNull(2)?.name shouldBe "Three \"and\" half"
//            }
//            "in snake_case" {
//                param.getOrNull(3)?.name shouldBe "three_and_more"
//            }
//            "with numbers" {
//                param.getOrNull(5)?.name shouldBe "arr01"
//            }
//        }
//
//        "type" - {
//            "text in lower case" {
//                param.getOrNull(0)?.type shouldBe "text"
//            }
//            "integer in UPPER case" {
//                param.getOrNull(1)?.type shouldBe "integer"
//            }
//            "character varying in two word" {
//                param.getOrNull(2)?.type shouldBe "character varying"
//            }
//            "character varying with max size" - {
//                "dont return the scale type in name" {
//                    param.getOrNull(3)?.type shouldBe "character varying"
//                }
//                "return the correct size" {
//                    param.getOrNull(3)?.type?.precision shouldBe 255
//                }
//            }
//            "numeric with precision and scale" - {
//                "dont return the precision and scale type in name" {
//                    param.getOrNull(5)?.type shouldBe "numeric"
//                }
//                "return the correct precision" {
//                    param.getOrNull(5)?.type?.precision shouldBe 10
//                }
//                "return the correct scale" {
//                    param.getOrNull(5)?.type?.scale shouldBe 3
//                }
//            }
//            "array of text" {
//                param.getOrNull(7)?.type shouldBe "text[]"
//            }
//        }
//        "default" - {
//            "with array of composite type Point" - {
//                """must return "arr02" at name""" {
//                    param.getOrNull(7)?.name shouldBe "arr02"
//                }
//                """must return "point[]" at type""" {
//                    param.getOrNull(7)?.type shouldBe "point[]"
//                }
//                """must return "array[(1, 2)::point, (7, 12)::point]::point[]" at default""" {
//                    param.getOrNull(7)?.default shouldBe "array[(1, 2)::point, (7, 12)::point]::point[]"
//                }
//            }
//        }
//    }
//
//    "getDefinition" - {
//        TODO("must be implement")
//    }
//
//    "getParametersIndexedByName" - {
//        TODO("must be implement")
//    }
//
//    "has same definition" - {
//        TODO("must be implement")
//    }
//
//    "is different from" - {
//        TODO("must be implement")
//    }
//
//    "script" - {
//        TODO("must be implement")
//    }
//
//    "source" - {
//        TODO("must be implement")
//    }
})
