grammar PL;

@header {
import backend.*;
}

@members {

}

program returns [Expr expr] : 
        { List <Expr> statements = new ArrayList<Expr>(); }
        (statement 
            { statements.add($statement.expr);} 
        )+ EOF
        { $expr = new Block (statements);}
        ;

statement returns [Expr expr]
    : assignment ';'                  { $expr = $assignment.expr; }
    | assignment                      { $expr = $assignment.expr; }
    | expression ';'                  { $expr = $expression.expr; }
    | expression                      { $expr = $expression.expr; }
    | loop                            { $expr = $loop.loopExpr;}
    | funcDef                         { $expr = $funcDef.funcExpr;}
    | invoke ';'                      { $expr = $invoke.invokeExpr;}
    | whileLoop                       { $expr = $whileLoop.whileLoopExpr;}
    ;

assignment returns [Expr expr]
    : 'let'? ID '=' expression               { $expr = new AssignmentExpr($ID.text, $expression.expr); }
    | ID '++'                                { $expr = new PlusPlus($ID.text); }
    |{
        List<Expr> values = new ArrayList<Expr>();
      } 
      name=ID '=' 'arrayOf' '(' 
         (expression { values.add($expression.expr); })
         (',' expression { values.add($expression.expr); })*
     ')'
     {
        $expr = new ArrayExpr($name.text, values);
     }
    | ID '[' n1=NUMERIC ']' '=' e1=expression
        { 
            int index = java.lang.Integer.parseInt($n1.getText());
            $expr = new ArrayReassign($ID.text, index, $e1.expr);
        }
    ;

expression returns [Expr expr]
    : '(' expression ')'                     { $expr = $expression.expr; }
    | NUMERIC                                { $expr = new IntLiteral($NUMERIC.text); }
    | STRING                                 { $expr = new StringExpr($STRING.text); }
    | ID                                     { $expr = new IdentifierExpr($ID.text); }
    | 'print(' expression ')'                { $expr = new PrintExpr($expression.expr); }
    | expression1=expression '++' expression2=expression {
        $expr = new ConcatenationExpr($expression1.expr, $expression2.expr);
    }
    | expression1=expression '*' expression2=expression {
        $expr = new MultiplyExpr($expression1.expr, $expression2.expr);
    }
    
    | expression1=expression '+' expression2=expression {
        $expr = new Arithmetics(Operator.Add,$expression1.expr, $expression2.expr);
    }
    | e1=expression '-' e2=expression                 {$expr = new Arithmetics(Operator.Sub, $e1.expr, $e2.expr);}
    | '-' e1=expression                               {$expr = new Arithmetics(Operator.Sub, new IntLiteral("0"), $e1.expr);}
    | e1=expression '*' e2=expression                 {$expr = new Arithmetics(Operator.Mul, $e1.expr, $e2.expr);}
    | e1=expression '<' e2=expression                 {$expr = new Compare(Comparator.LT, $e1.expr, $e2.expr);}
    | invoke                                          {$expr = $invoke.invokeExpr;}
    | {
        List<Expr> ifBody = new ArrayList<Expr>();
        List<Expr> elseBody = new ArrayList<Expr>();
      }
      'if' '(' cond = expression ')' '{' ifStmt=statement '}' 'else' '{' elseStmt=statement '}' 
      {$expr = new Ifelse($cond.expr, $ifStmt.expr, $elseStmt.expr);}
    | ID '[' n1=NUMERIC ']' 
        { 
            int index = java.lang.Integer.parseInt($n1.getText());
            $expr = new ArrayAccess($ID.text, index);
        }
    ;

loop returns [Expr loopExpr]:
    {
        List<Expr> body = new ArrayList<Expr>();
    }
    'for' '(' ID 'in' n1=NUMERIC '..' n2=NUMERIC ')' '{'
        (statement { body.add($statement.expr); })*
    '}'
    {
        // Parse numeric values and convert them to integers
        int start = java.lang.Integer.parseInt($n1.getText());
        int end = java.lang.Integer.parseInt($n2.getText());

        // Create a loop expression with the specified range and body
        $loopExpr = new ForLoopExpr($ID.text, start, end, body);
    };
    
whileLoop returns [Expr whileLoopExpr]:
    {
        List<Expr> body = new ArrayList<Expr>();
    }
    'while' '(' e1=expression '<' e2=expression')' '{'
        (statement { body.add($statement.expr); })*
    '}'
    {

        // Create a loop expression with the specified range and body
        $whileLoopExpr = new While(new Compare(Comparator.LT, $e1.expr, $e2.expr), new Block(body));
    };

funcDef returns [Expr funcExpr]:
    {
        List<Expr> funcBody = new ArrayList<Expr>();
        List<String> params = new ArrayList<String>();
        
    }
     'function' name=ID '(' 
     (ID { params.add($ID.text); })?
     (',' ID { params.add($ID.text); })* 
     ')' 
     '{' (statement { funcBody.add($statement.expr); })+ '}' 
    {

        $funcExpr = new Declare($name.text, params, new Block(funcBody));
    }
    ;

invoke returns [Expr invokeExpr]:
    {
        List<Expr> args = new ArrayList<Expr>();
        
    }
     ID '(' (expression { args.add($expression.expr); })*
     (',' expression { args.add($expression.expr); })*  ')'
     {
     $invokeExpr = new Invoke($ID.text, args);
     }
    ;


NUMERIC : ('0' .. '9')+;
STRING : '"' (~["\r\n])* '"'
       {setText(getText().substring(1,getText().length()-1));}
       ;
BOOLEAN : 'true' | 'false';
ID : ('a' .. 'z' | 'A' .. 'Z' | '_') ('a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_')*;
COMMENT : '/*' .*? '*/' -> skip;
WHITESPACE : (' ' | '\t' | '\r' | '\n')+ -> skip;
