/**
 * @Author: turk
 * @Description: Sintaksni analizator.
 */

package compiler.parser;

import static compiler.lexer.TokenType.*;
import static common.RequireNonNull.requireNonNull;

import java.io.PrintStream;
import java.util.List;
import java.util.Optional;

import common.Report;
import compiler.lexer.Position;
import compiler.lexer.Symbol;
import compiler.lexer.TokenType;

public class Parser {
    /**
     * Seznam leksikalnih simbolov.
     */
    private final List<Symbol> symbols;

    /**
     * Ciljni tok, kamor izpisujemo produkcije. Če produkcij ne želimo izpisovati,
     * vrednost opcijske spremenljivke nastavimo na Optional.empty().
     */
    private final Optional<PrintStream> productionsOutputStream;

    public Parser(List<Symbol> symbols, Optional<PrintStream> productionsOutputStream) {
        requireNonNull(symbols, productionsOutputStream);
        this.symbols = symbols;
        this.productionsOutputStream = productionsOutputStream;
    }

    /**
     * Izvedi sintaksno analizo.
     */
    public void parse() {
        parseSource();
    }

    private void parseSource() {
        parseDefinitions();
    }

    /**
     * Izpiše produkcijo na izhodni tok.
     */
    private void dump(String production) {
        if (productionsOutputStream.isPresent()) {
            productionsOutputStream.get().println(production);
        }
    }

    private boolean check(TokenType token) {
        return symbols.get(0).tokenType == token;
    }

    private void skip() {
        symbols.remove(0);
    }

    /**
     * Implementacija rekurzivnega spuščanja
     */

    private void parseDefinitions() {
        dump("definitions -> definition definitions_");
        parseDefinition();
        parseDefinitions_();
    }

    private void parseDefinitions_() {
        if (check(OP_SEMICOLON)) {
            // skip ';'
            skip();
            dump("definitions_ -> ; definitions");
            parseDefinitions();
            return;
        }
        dump("definitions_ -> .");
    }

    private void parseDefinition() {
        if (check(KW_TYP)) {
            dump("definition -> type_definition");
            parseTypeDef();
        } else if (check(KW_FUN)) {
            dump("definition -> function_definition");
            parseFunctionDef();
        } else if (check(KW_VAR)) {
            dump("definition -> variable_definition");
            parseVarDef();
        } else {
            // TODO: throw error if there is something else in place of definition type
        }
    }

    private void parseTypeDef() {
        // TODO: check for errors for wrong implementation of type definition
        // skip KW_TYP
        skip();
        // skip IDENTIFIER
        skip();
        // skip ':'
        skip();
        dump("type_definition -> typ identifier : type");
        parseType();
    }

    private void parseType() {
        if (check(IDENTIFIER)) {
            dump("type -> identifier");
            // skip identifier
            skip();
        } else if (check(AT_LOGICAL)) {
            dump("type -> logical");
            // skip logical
            skip();
        } else if (check(AT_INTEGER)) {
            dump("type -> integer");
            // skip integer
            skip();
        } else if (check(AT_STRING)) {
            dump("type -> string");
            // skip string
            skip();
        } else if (check(KW_ARR)) {
            // TODO: check for wrong array implementation
            dump("type -> arr [ int_const ] type");
            // skip 'arr'
            skip();
            // skip '['
            skip();
            // skip 'int_const'
            skip();
            // skip ']'
            skip();
            parseType();
        } else {
            // TODO: throw error if there is something else in the place of type
        }
    }

    private void parseFunctionDef() {
        // TODO: throw error for wrong function definition
        dump("function_definition -> 'fun' 'identifier' '(' parameters ')' ':' type '=' expression");
        // skip fun
        skip();
        // skip identifier
        skip();
        // skip (
        skip();
        parseParameters();
        // skip )
        skip();
        // skip :
        skip();
        parseType();
        // skip =
        skip();
        parseExpression();
    }

    private void parseParameters() {
        dump("parameters -> parameter parameters_");
        parseParameter();
        parseParameters_();
    }

    private void parseParameters_() {
        if (check(OP_COMMA)) {
            dump("parameters_ -> , parameters");
            // skip ,
            skip();
            parseParameters();
            return;
        }
        dump("parameters_ -> .");
    }

    private void parseParameter() {
        // TODO: check for wrong parameter use
        // skip identifier
        skip();
        // skip :
        skip();
        dump("parameter -> identifier : type");
        parseType();
    }

    private void parseExpression() {
        dump("expression -> logical_ior_expression expression_");
        parseLogicalIorExpression();
        parseExpression_();
    }

    private void parseExpression_() {
        // TODO: check for errors in expression
        if (check(OP_LBRACE)) {
            dump("expression_ -> { WHERE definitions }");
            // skip {
            skip();
            // skip where
            skip();
            parseDefinitions();
            // skip }
            skip();
            return;
        }
        dump("expression_ -> .");
    }

    private void parseLogicalIorExpression() {
        dump("logical_ior_expression -> logical_and_expression logical_ior_expression_");
        parseLogicalAndExpression();
        parseLogicalIorExpression_();
    }

    private void parseLogicalIorExpression_(){
        if (check(OP_OR)) {
            dump("logical_ior_expression_ -> | logical_ior_expression");
            // skip |
            skip();
            parseLogicalIorExpression();
        }
        dump("logical_ior_expression_ -> .");
    }

    private void parseLogicalAndExpression() {
        dump("logical_ior_expression -> logical_and_expression logical_ior_expression_");
        parseCompareExpression();
        parseLogicalAndExpression_();
    }

    private void parseLogicalAndExpression_() {
        if (check(OP_AND)) {
            dump("logical_and_expression_ -> & logical_and_expression");
            // skip &
            skip();
            parseLogicalAndExpression();
        }
        dump("logical_and_expression_ -> .");
    }

    private void parseCompareExpression() {
        dump("compare_expression -> additive_expression compare_expression");
        parseAdditiveExpression();
        parseCompareExpression_();
    }

    private void parseCompareExpression_() {
        if (check(OP_EQ)) {
            dump("compare_expression_ -> == additive_expression");
            // skip ==
            skip();
            parseAdditiveExpression();
        } else if (check(OP_NEQ)) {
            dump("compare_expression_ -> != additive_expression");
            // skip !=
            skip();
            parseAdditiveExpression();
        } else if (check(OP_NEQ)) {
            dump("compare_expression_ -> <= additive_expression");
            // skip <=
            skip();
            parseAdditiveExpression();
        } else if (check(OP_NEQ)) {
            dump("compare_expression_ -> >= additive_expression");
            // skip >=
            skip();
            parseAdditiveExpression();
        } else if (check(OP_NEQ)) {
            dump("compare_expression_ -> < additive_expression");
            // skip <
            skip();
            parseAdditiveExpression();
        } else if (check(OP_NEQ)) {
            dump("compare_expression_ -> > additive_expression");
            // skip >
            skip();
            parseAdditiveExpression();
        } else {
            dump("compare_expression_ -> .");
        }
    }

    private void parseAdditiveExpression() {
        dump("additive_expression -> multiplicative_expression additive_expression_");
        parseMultiplicativeExpression();
        parseAdditiveExpression_();
    }

    private void parseAdditiveExpression_() {
        if (check(OP_ADD)) {
            dump("additive_expression_ -> + additive_expression");
            // skip +
            skip();
            parseAdditiveExpression();
        } else if (check(OP_SUB)) {
            dump("additive_expression_ -> - additive_expression");
            // skip -
            skip();
            parseAdditiveExpression();
        } else {
            dump("additive_expression_ -> .");
        }
    }

    private void parseMultiplicativeExpression() {
        dump("multiplicative_expression -> prefix_expression multiplicative_expression_");
        parsePrefixExpression();
        parseMultiplicativeExpression_();
    }

    private void parseMultiplicativeExpression_() {
        if (check(OP_ADD)) {
            dump("multiplicative_expression_ -> + multiplicative_expression");
            // skip +
            skip();
            parseMultiplicativeExpression();
        } else if (check(OP_SUB)) {
            dump("multiplicative_expression_ -> - multiplicative_expression");
            // skip -
            skip();
            parseMultiplicativeExpression();
        } else if (check(OP_MOD)) {
            dump("multiplicative_expression_ -> % multiplicative_expression");
            // skip %
            skip();
            parseMultiplicativeExpression();
        } else {
            dump("multiplicative_expression_ -> .");
        }
    }

    private void parsePrefixExpression() {
        if (check(OP_ADD)) {
            dump("prefix_expression -> + prefix_expression");
            // skip +
            skip();
            parsePrefixExpression();
        } else if (check(OP_SUB)) {
            dump("prefix_expression -> - prefix_expression");
            // skip -
            skip();
            parsePrefixExpression();
        } else if (check(OP_NOT)) {
            dump("prefix_expression -> ! prefix_expression");
            // skip !
            skip();
            parsePrefixExpression();
        } else {
            dump("prefix_expression -> postfix_expression");
            parsePostdfixExpression();
        }
    }

    private void parsePostdfixExpression() {
        dump("postfix_expression -> atom_expression postfix_expression_");
        parseAtomExpression();
        parsePostdfixExpression_();
    }

    private void parsePostdfixExpression_() {
        if (check(OP_LBRACKET)) {
            // TODO: check for error
            dump("postfix_expression -> [ expression ] postfix_expression");
            // skip [
            skip();
            parseExpression();
            // skip ]
            skip();
            parsePostdfixExpression();
        } else {
            dump("postfix_expression -> .");
        }
    }

    private void parseAtomExpression() {
        if (check(C_LOGICAL)) {

        } else if (check(C_INTEGER)) {

        } else if (check(C_STRING)) {
            
        } else if (check(IDENTIFIER)) {
            
        } else if (check(OP_LBRACE)) {

        }
    }

    private void parseExpressions() {
        dump("expressions -> expression expressions_");
        parseExpression();
        parseExpressions_();
    }

    private void parseExpressions_() {
        if (check(OP_COMMA)) {
            dump("expressions_ -> , expressions");
            // skip ,
            skip();
            parseExpressions();
        } else {
            dump("expressions_ -> .");
        }
    }

    private void parseVarDef() {
        // TODO: check for errors in var def
        // skip var
        skip();
        // skip identifier
        skip();
        // skip :
        skip();
        parseType();
    }
}
