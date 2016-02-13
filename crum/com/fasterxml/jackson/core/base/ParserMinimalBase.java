package com.fasterxml.jackson.core.base;

import android.support.v4.view.MotionEventCompat;
import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.io.NumberInput;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.core.util.VersionUtil;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer.Std;
import java.io.IOException;
import org.apache.commons.codec.CharEncoding;

public abstract class ParserMinimalBase extends JsonParser {
    protected static final int INT_BACKSLASH = 92;
    protected static final int INT_COLON = 58;
    protected static final int INT_COMMA = 44;
    protected static final int INT_CR = 13;
    protected static final int INT_E = 69;
    protected static final int INT_HASH = 35;
    protected static final int INT_LBRACKET = 91;
    protected static final int INT_LCURLY = 123;
    protected static final int INT_LF = 10;
    protected static final int INT_PERIOD = 46;
    protected static final int INT_QUOTE = 34;
    protected static final int INT_RBRACKET = 93;
    protected static final int INT_RCURLY = 125;
    protected static final int INT_SLASH = 47;
    protected static final int INT_SPACE = 32;
    protected static final int INT_TAB = 9;
    protected static final int INT_e = 101;
    protected JsonToken _currToken;
    protected JsonToken _lastClearedToken;

    protected abstract void _handleEOF() throws JsonParseException;

    public abstract void close() throws IOException;

    public abstract byte[] getBinaryValue(Base64Variant base64Variant) throws IOException;

    public abstract String getCurrentName() throws IOException;

    public abstract JsonStreamContext getParsingContext();

    public abstract String getText() throws IOException;

    public abstract char[] getTextCharacters() throws IOException;

    public abstract int getTextLength() throws IOException;

    public abstract int getTextOffset() throws IOException;

    public abstract boolean hasTextCharacters();

    public abstract boolean isClosed();

    public abstract JsonToken nextToken() throws IOException, JsonParseException;

    public abstract void overrideCurrentName(String str);

    protected ParserMinimalBase() {
    }

    protected ParserMinimalBase(int features) {
        super(features);
    }

    public JsonToken getCurrentToken() {
        return this._currToken;
    }

    public final int getCurrentTokenId() {
        JsonToken t = this._currToken;
        return t == null ? 0 : t.id();
    }

    public boolean hasCurrentToken() {
        return this._currToken != null;
    }

    public JsonToken nextValue() throws IOException {
        JsonToken t = nextToken();
        if (t == JsonToken.FIELD_NAME) {
            return nextToken();
        }
        return t;
    }

    public JsonParser skipChildren() throws IOException {
        if (this._currToken == JsonToken.START_OBJECT || this._currToken == JsonToken.START_ARRAY) {
            int open = 1;
            while (true) {
                JsonToken t = nextToken();
                if (t == null) {
                    break;
                } else if (t.isStructStart()) {
                    open++;
                } else if (t.isStructEnd()) {
                    open--;
                    if (open == 0) {
                        break;
                    }
                } else {
                    continue;
                }
            }
            _handleEOF();
        }
        return this;
    }

    public void clearCurrentToken() {
        if (this._currToken != null) {
            this._lastClearedToken = this._currToken;
            this._currToken = null;
        }
    }

    public JsonToken getLastClearedToken() {
        return this._lastClearedToken;
    }

    public boolean getValueAsBoolean(boolean defaultValue) throws IOException {
        JsonToken t = this._currToken;
        if (t != null) {
            switch (t.id()) {
                case Std.STD_CURRENCY /*6*/:
                    String str = getText().trim();
                    if ("true".equals(str)) {
                        return true;
                    }
                    if ("false".equals(str)) {
                        return false;
                    }
                    if (_hasTextualNull(str)) {
                        return false;
                    }
                    break;
                case Std.STD_PATTERN /*7*/:
                    if (getIntValue() == 0) {
                        return false;
                    }
                    return true;
                case INT_TAB /*9*/:
                    return true;
                case INT_LF /*10*/:
                case Std.STD_INET_ADDRESS /*11*/:
                    return false;
                case Std.STD_INET_SOCKET_ADDRESS /*12*/:
                    Object value = getEmbeddedObject();
                    if (value instanceof Boolean) {
                        return ((Boolean) value).booleanValue();
                    }
                    break;
            }
        }
        return defaultValue;
    }

    public int getValueAsInt(int defaultValue) throws IOException {
        JsonToken t = this._currToken;
        if (t == null) {
            return defaultValue;
        }
        switch (t.id()) {
            case Std.STD_CURRENCY /*6*/:
                String str = getText();
                if (_hasTextualNull(str)) {
                    return 0;
                }
                return NumberInput.parseAsInt(str, defaultValue);
            case Std.STD_PATTERN /*7*/:
            case Std.STD_LOCALE /*8*/:
                return getIntValue();
            case INT_TAB /*9*/:
                return 1;
            case INT_LF /*10*/:
                return 0;
            case Std.STD_INET_ADDRESS /*11*/:
                return 0;
            case Std.STD_INET_SOCKET_ADDRESS /*12*/:
                Object value = getEmbeddedObject();
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
                return defaultValue;
            default:
                return defaultValue;
        }
    }

    public long getValueAsLong(long defaultValue) throws IOException {
        JsonToken t = this._currToken;
        if (t == null) {
            return defaultValue;
        }
        switch (t.id()) {
            case Std.STD_CURRENCY /*6*/:
                String str = getText();
                if (_hasTextualNull(str)) {
                    return 0;
                }
                return NumberInput.parseAsLong(str, defaultValue);
            case Std.STD_PATTERN /*7*/:
            case Std.STD_LOCALE /*8*/:
                return getLongValue();
            case INT_TAB /*9*/:
                return 1;
            case INT_LF /*10*/:
            case Std.STD_INET_ADDRESS /*11*/:
                return 0;
            case Std.STD_INET_SOCKET_ADDRESS /*12*/:
                Object value = getEmbeddedObject();
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
                return defaultValue;
            default:
                return defaultValue;
        }
    }

    public double getValueAsDouble(double defaultValue) throws IOException {
        JsonToken t = this._currToken;
        if (t == null) {
            return defaultValue;
        }
        switch (t.id()) {
            case Std.STD_CURRENCY /*6*/:
                String str = getText();
                if (_hasTextualNull(str)) {
                    return 0.0d;
                }
                return NumberInput.parseAsDouble(str, defaultValue);
            case Std.STD_PATTERN /*7*/:
            case Std.STD_LOCALE /*8*/:
                return getDoubleValue();
            case INT_TAB /*9*/:
                return 1.0d;
            case INT_LF /*10*/:
            case Std.STD_INET_ADDRESS /*11*/:
                return 0.0d;
            case Std.STD_INET_SOCKET_ADDRESS /*12*/:
                Object value = getEmbeddedObject();
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                return defaultValue;
            default:
                return defaultValue;
        }
    }

    public String getValueAsString(String defaultValue) throws IOException {
        return (this._currToken == JsonToken.VALUE_STRING || !(this._currToken == null || this._currToken == JsonToken.VALUE_NULL || !this._currToken.isScalarValue())) ? getText() : defaultValue;
    }

    protected void _decodeBase64(String str, ByteArrayBuilder builder, Base64Variant b64variant) throws IOException {
        try {
            b64variant.decode(str, builder);
        } catch (IllegalArgumentException e) {
            _reportError(e.getMessage());
        }
    }

    @Deprecated
    protected void _reportInvalidBase64(Base64Variant b64variant, char ch, int bindex, String msg) throws JsonParseException {
        String base;
        if (ch <= ' ') {
            base = "Illegal white space character (code 0x" + Integer.toHexString(ch) + ") as character #" + (bindex + 1) + " of 4-char base64 unit: can only used between units";
        } else if (b64variant.usesPaddingChar(ch)) {
            base = "Unexpected padding character ('" + b64variant.getPaddingChar() + "') as character #" + (bindex + 1) + " of 4-char base64 unit: padding only legal as 3rd or 4th character";
        } else if (!Character.isDefined(ch) || Character.isISOControl(ch)) {
            base = "Illegal character (code 0x" + Integer.toHexString(ch) + ") in base64 content";
        } else {
            base = "Illegal character '" + ch + "' (code 0x" + Integer.toHexString(ch) + ") in base64 content";
        }
        if (msg != null) {
            base = base + ": " + msg;
        }
        throw _constructError(base);
    }

    @Deprecated
    protected void _reportBase64EOF() throws JsonParseException {
        throw _constructError("Unexpected end-of-String in base64 content");
    }

    protected boolean _hasTextualNull(String value) {
        return "null".equals(value);
    }

    protected void _reportUnexpectedChar(int ch, String comment) throws JsonParseException {
        if (ch < 0) {
            _reportInvalidEOF();
        }
        String msg = "Unexpected character (" + _getCharDesc(ch) + ")";
        if (comment != null) {
            msg = msg + ": " + comment;
        }
        _reportError(msg);
    }

    protected void _reportInvalidEOF() throws JsonParseException {
        _reportInvalidEOF(" in " + this._currToken);
    }

    protected void _reportInvalidEOF(String msg) throws JsonParseException {
        _reportError("Unexpected end-of-input" + msg);
    }

    protected void _reportInvalidEOFInValue() throws JsonParseException {
        _reportInvalidEOF(" in a value");
    }

    protected void _reportMissingRootWS(int ch) throws JsonParseException {
        _reportUnexpectedChar(ch, "Expected space separating root-level values");
    }

    protected void _throwInvalidSpace(int i) throws JsonParseException {
        _reportError("Illegal character (" + _getCharDesc((char) i) + "): only regular white space (\\r, \\n, \\t) is allowed between tokens");
    }

    protected void _throwUnquotedSpace(int i, String ctxtDesc) throws JsonParseException {
        if (!isEnabled(Feature.ALLOW_UNQUOTED_CONTROL_CHARS) || i > INT_SPACE) {
            _reportError("Illegal unquoted character (" + _getCharDesc((char) i) + "): has to be escaped using backslash to be included in " + ctxtDesc);
        }
    }

    protected char _handleUnrecognizedCharacterEscape(char ch) throws JsonProcessingException {
        if (!(isEnabled(Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER) || (ch == '\'' && isEnabled(Feature.ALLOW_SINGLE_QUOTES)))) {
            _reportError("Unrecognized character escape " + _getCharDesc(ch));
        }
        return ch;
    }

    protected static final String _getCharDesc(int ch) {
        char c = (char) ch;
        if (Character.isISOControl(c)) {
            return "(CTRL-CHAR, code " + ch + ")";
        }
        if (ch > MotionEventCompat.ACTION_MASK) {
            return "'" + c + "' (code " + ch + " / 0x" + Integer.toHexString(ch) + ")";
        }
        return "'" + c + "' (code " + ch + ")";
    }

    protected final void _reportError(String msg) throws JsonParseException {
        throw _constructError(msg);
    }

    protected final void _wrapError(String msg, Throwable t) throws JsonParseException {
        throw _constructError(msg, t);
    }

    protected final void _throwInternal() {
        VersionUtil.throwInternal();
    }

    protected final JsonParseException _constructError(String msg, Throwable t) {
        return new JsonParseException(msg, getCurrentLocation(), t);
    }

    protected static byte[] _asciiBytes(String str) {
        byte[] b = new byte[str.length()];
        int len = str.length();
        for (int i = 0; i < len; i++) {
            b[i] = (byte) str.charAt(i);
        }
        return b;
    }

    protected static String _ascii(byte[] b) {
        try {
            return new String(b, CharEncoding.US_ASCII);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
