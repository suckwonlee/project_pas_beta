package com.pas.game.multiplayer;

import java.util.LinkedHashMap;
import java.util.Map;

/** 외부 라이브러리 없이 멀티 명령의 단순 JSON 객체를 읽고 쓰는 작은 유틸리티. */
final class WireJson {
    private WireJson(){}

    static String quote(String value){
        if(value==null)return "null";
        StringBuilder out=new StringBuilder("\"");
        for(int i=0;i<value.length();i++){
            char c=value.charAt(i);
            switch(c){
                case '\"':out.append("\\\"");break;
                case '\\':out.append("\\\\");break;
                case '\b':out.append("\\b");break;
                case '\f':out.append("\\f");break;
                case '\n':out.append("\\n");break;
                case '\r':out.append("\\r");break;
                case '\t':out.append("\\t");break;
                default:if(c<0x20)out.append(String.format("\\u%04x",(int)c));else out.append(c);
            }
        }
        return out.append('\"').toString();
    }

    static Map<String,String> parseFlatObject(String json){
        if(json==null)throw new IllegalArgumentException("JSON payload is null");
        Parser parser=new Parser(json);Map<String,String> result=new LinkedHashMap<>();
        parser.space();parser.expect('{');parser.space();
        if(parser.peek('}')){parser.expect('}');parser.end();return result;}
        while(true){
            String key=parser.string();parser.space();parser.expect(':');parser.space();
            String value=parser.peek('\"')?parser.string():parser.literal();result.put(key,value);
            parser.space();if(parser.peek('}')){parser.expect('}');parser.end();return result;}parser.expect(',');parser.space();
        }
    }

    private static final class Parser {
        private final String text;private int index;
        Parser(String text){this.text=text;}
        void space(){while(index<text.length()&&Character.isWhitespace(text.charAt(index)))index++;}
        boolean peek(char value){return index<text.length()&&text.charAt(index)==value;}
        void expect(char value){if(!peek(value))throw new IllegalArgumentException("Malformed JSON near position "+index);index++;}
        String string(){
            expect('\"');StringBuilder out=new StringBuilder();
            while(index<text.length()){
                char c=text.charAt(index++);if(c=='\"')return out.toString();
                if(c!='\\'){out.append(c);continue;}
                if(index>=text.length())break;char escaped=text.charAt(index++);
                switch(escaped){
                    case '\"':out.append('\"');break;case '\\':out.append('\\');break;case '/':out.append('/');break;
                    case 'b':out.append('\b');break;case 'f':out.append('\f');break;case 'n':out.append('\n');break;case 'r':out.append('\r');break;case 't':out.append('\t');break;
                    case 'u':if(index+4>text.length())throw new IllegalArgumentException("Malformed unicode escape");out.append((char)Integer.parseInt(text.substring(index,index+4),16));index+=4;break;
                    default:throw new IllegalArgumentException("Malformed escape near position "+index);
                }
            }
            throw new IllegalArgumentException("Unterminated JSON string");
        }
        String literal(){int start=index;while(index<text.length()&&",}".indexOf(text.charAt(index))<0)index++;String value=text.substring(start,index).trim();if(value.isEmpty())throw new IllegalArgumentException("Missing JSON value");return "null".equals(value)?null:value;}
        void end(){space();if(index!=text.length())throw new IllegalArgumentException("Trailing JSON content");}
    }
}
