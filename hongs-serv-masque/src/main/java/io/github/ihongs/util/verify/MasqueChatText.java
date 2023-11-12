package io.github.ihongs.util.verify;

import io.github.ihongs.CruxException;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.util.Synt;
import java.util.Map;

/**
 * 判断并标记非文本
 * @author hong
 */
public class MasqueChatText extends Rule {

    @Override
    public Object verify(Value watch) throws Wrong {
        Object val  = watch.get();
        Map    vals = watch.getValues();
        String kind = Synt.declare(vals.get("kind"), "text");
        switch(kind) {
            case "link" :
            case "file" :
            case "image":
            case "video":
            case "voice":
                if (val == null || "".equals(val)) {
                try {
                    return "["+ FormSet.getInstance ("masque")
                                        .getEnum ("chat_kind")
                                        .get     ( kind ) +"]";
                } catch (CruxException e) {
                  throw  e.toExemption( );
                } }
                return val;
            default:
                if (val == null || "".equals(val)) {
                    throw  new  Wrong  ("@fore.form.required");
                }
                return val;
        }
    }

}
