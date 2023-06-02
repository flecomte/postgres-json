package fr.postgresjson.definition

import fr.postgresjson.definition.Parameter.Direction.IN
import fr.postgresjson.definition.Parameter.Direction.INOUT
import fr.postgresjson.definition.Parameter.Direction.OUT
import fr.postgresjson.definition.Returns.Primitive
import fr.postgresjson.definition.parse.parseFunction
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.amshove.kluent.shouldBeInstanceOf

class FunctionTest : FreeSpec({
    "Function name" - {
        "all in lower" {
            parseFunction(
                // language=PostgreSQL
                """
                create or replace function myfun() returns text language plpgsql as
                $$ begin; end$$;
                """.trimIndent()
            ).apply {
                name shouldBe "myfun"
            }
        }

        "first letter caps without quoted" {
            parseFunction(
                // language=PostgreSQL
                """
                create or replace function Myfun() returns text language plpgsql as
                $$ begin; end$$;
                """.trimIndent()
            ).apply {
                name shouldBe "myfun"
            }
        }

        "with numbers" {
            parseFunction(
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
            parseFunction(
                // language=PostgreSQL
                """
                create or replace function "My fun"() returns text language plpgsql as
                $$ begin; end$$;
                """.trimIndent()
            ).apply {
                name shouldBe "My fun"
            }
        }

        "quoted name with double quote in name" {
            parseFunction(
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
            parseFunction(
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
            val param = parseFunction(
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
            val param = parseFunction(
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

        "Escaped parameters name" - {
            val param = parseFunction(
                // language=PostgreSQL
                """
                create or replace function myfun("one""or two" text, "#@€" int) returns text language plpgsql as
                $$ begin end;$$;
                """.trimIndent()
            ).parameters

            "should have 2 parameters" {
                param shouldHaveSize 2
            }

            "should have names" {
                param[0].name shouldBe "one\"or two"
                param[1].name shouldBe "#@€"
            }
        }
        "Parameters with Caps" - {
            val param = parseFunction(
                // language=PostgreSQL
                """
                create or replace function myfun("One" text, Two text) returns text language plpgsql as
                $$ begin end;$$;
                """.trimIndent()
            ).parameters

            "should have first parameter name" {
                param[0].name shouldBe "One"
                param[1].name shouldBe "two"
            }
        }

        "Parameters with type `character varying(255)`" - {
            val param = parseFunction(
                // language=PostgreSQL
                """
                create or replace function myfun(one character varying(255)) returns text language plpgsql as
                $$ begin end;$$;
                """.trimIndent()
            ).parameters

            "should have 1 parameters" {
                param shouldHaveSize 1
            }

            "should have name" {
                param[0].name shouldBe "one"
            }

            "should have type name" {
                param[0].type.name shouldBe "character varying"
            }

            "should have type precision" {
                param[0].type.precision shouldBe 255
                param[0].type.scale shouldBe null
            }
        }

        "Parameters with type `numeric(16, 8)`" - {
            val param = parseFunction(
                // language=PostgreSQL
                """
                create or replace function myfun(one numeric(16, 8)) returns text language plpgsql as
                $$ begin end;$$;
                """.trimIndent()
            ).parameters

            "should have 1 parameters" {
                param shouldHaveSize 1
            }

            "should have name" {
                param[0].name shouldBe "one"
            }

            "should have type name" {
                param[0].type.name shouldBe "numeric"
            }

            "should have type precision" {
                param[0].type.precision shouldBe 16
            }

            "should have type scale" {
                param[0].type.scale shouldBe 8
            }
        }

        "Parameters with default text" - {
            val param = parseFunction(
                // language=PostgreSQL
                """
                create or replace function myfun(one text default 'example') returns text language plpgsql as
                $$ begin end;$$;
                """.trimIndent()
            ).parameters

            "should have 1 parameters" {
                param shouldHaveSize 1
            }

            "should have name" {
                param[0].name shouldBe "one"
            }

            "should have type name" {
                param[0].type.name shouldBe "text"
            }

            "should have default text" {
                param[0].default shouldBe "'example'"
            }
        }

        "Parameters with default int" - {
            val param = parseFunction(
                // language=PostgreSQL
                """
                create or replace function myfun(one int DEFAULT 123456 ) returns text language plpgsql as
                $$ begin end;$$;
                """.trimIndent()
            ).parameters

            "should have 1 parameters" {
                param shouldHaveSize 1
            }

            "should have name" {
                param[0].name shouldBe "one"
            }

            "should have type name" {
                param[0].type.name shouldBe "int"
            }

            "should have default text" {
                param[0].default shouldBe "123456"
            }
        }

        "Parameters with multiple default and equal" - {
            val param = parseFunction(
                // language=PostgreSQL
                """
                create or replace function myfun(one int DEFAULT 123456 , two text default 'hello', three text = '654') returns text language plpgsql as
                $$ begin end;$$;
                """.trimIndent()
            ).parameters

            "should have 3 parameters" {
                param shouldHaveSize 3
            }

            "should have name" {
                param[0].name shouldBe "one"
                param[1].name shouldBe "two"
                param[2].name shouldBe "three"
            }

            "should have type name" {
                param[0].type.name shouldBe "int"
                param[1].type.name shouldBe "text"
                param[2].type.name shouldBe "text"
            }

            "should have default text" {
                param[0].default shouldBe "123456"
                param[1].default shouldBe "'hello'"
                param[2].default shouldBe "'654'"
            }
        }

        "parameters with IN OUT INOUT" - {
            val param = parseFunction(
                // language=PostgreSQL
                """
                create or replace function myfun(in one text, inout two text, out three text, four text) language plpgsql as
                $$ begin end;$$;
                """.trimIndent()
            ).parameters

            "should have 4 parameters" {
                param shouldHaveSize 4
            }

            "should have parameter name" {
                param[0].name shouldBe "one"
                param[1].name shouldBe "two"
                param[2].name shouldBe "three"
                param[3].name shouldBe "four"
            }

            "should have parameter type name" {
                param[0].type.name shouldBe "text"
                param[1].type.name shouldBe "text"
                param[2].type.name shouldBe "text"
            }

            "should have parameter direction" {
                param[0].direction shouldBe IN
                param[1].direction shouldBe INOUT
                param[2].direction shouldBe OUT
                param[3].direction shouldBe IN
            }
        }

        "Parameters with type array of numeric" - {
            val param = parseFunction(
                // language=PostgreSQL
                """
                create or replace function myfun(one numeric(10, 2)[]) language plpgsql as
                $$ begin end;$$;
                """.trimIndent()
            ).parameters

            "should have 1 parameters" {
                param shouldHaveSize 1
            }

            "should have parameter name" {
                param[0].name shouldBe "one"
            }

            "should have parameter type is array" {
                param[0].type.isArray shouldBe true
            }

            "should have parameter type name" {
                param[0].type.name shouldBe "numeric"
            }

            "should have parameter type precision" {
                param[0].type.precision shouldBe 10
            }

            "should have parameter type scale" {
                param[0].type.scale shouldBe 2
            }
        }

        "Parameters with type array of text" - {
            val param = parseFunction(
                // language=PostgreSQL
                """
                create or replace function myfun(one text[], two int[], three text) language plpgsql as
                $$ begin end;$$;
                """.trimIndent()
            ).parameters

            "should have 2 parameters" {
                param shouldHaveSize 3
            }

            "should have parameter name" {
                param[0].name shouldBe "one"
                param[1].name shouldBe "two"
                param[2].name shouldBe "three"
            }

            "should have parameter type is array" {
                param[0].type.isArray shouldBe true
                param[1].type.isArray shouldBe true
                param[2].type.isArray shouldBe false
            }

            "should have parameter type name" {
                param[0].type.name shouldBe "text"
                param[1].type.name shouldBe "int"
                param[2].type.name shouldBe "text"
            }

            "should have parameter direction" {
                param[0].direction shouldBe IN
                param[1].direction shouldBe IN
                param[2].direction shouldBe IN
            }
        }

        "Parameters with type array multidimensional of text" - {
            val param = parseFunction(
                // language=PostgreSQL
                """
                create or replace function myfun(one text[][]) language plpgsql as
                $$ begin end;$$;
                """.trimIndent()
            ).parameters

            "should have parameter type is array" {
                param[0].type.isArray shouldBe true
            }

            "should have parameter type name" {
                param[0].type.name shouldBe "text"
            }
        }

        "Parameters with type fixed size array" - {
            val param = parseFunction(
                // language=PostgreSQL
                """
                create or replace function myfun(one text[45], two text[1][]) language plpgsql as
                $$ begin end;$$;
                """.trimIndent()
            ).parameters

            "should have parameter type is array" {
                param[0].type.isArray shouldBe true
                param[1].type.isArray shouldBe true
            }

            "should have parameter type name" {
                param[0].type.name shouldBe "text"
                param[1].type.name shouldBe "text"
            }
        }
    }

    "Function Returns" - {
        "should return the type text" {
            val returns = parseFunction(
                // language=PostgreSQL
                """
                create or replace function myfun() returns text language plpgsql as 
                $$ begin; end$$;
                """.trimIndent()
            ).returns

            returns shouldBeInstanceOf Primitive::class
            returns.definition shouldBe "text"
            returns.isSetOf shouldBe false
        }

        "should return the type character varying" {
            val returns = parseFunction(
                // language=PostgreSQL
                """
                create or replace function myfun() returns character varying language plpgsql as 
                $$ begin; end$$;
                """.trimIndent()
            ).returns

            returns shouldBeInstanceOf Primitive::class
            returns.definition shouldBe "character varying"
            returns.isSetOf shouldBe false
        }

        "should return the type character varying(255)" {
            val returns = parseFunction(
                // language=PostgreSQL
                """
                create or replace function myfun() returns character varying(255) language plpgsql as 
                $$ begin; end$$;
                """.trimIndent()
            ).returns

            returns shouldBeInstanceOf Primitive::class
            returns.definition shouldBe "character varying(255)"
            returns.isSetOf shouldBe false
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
