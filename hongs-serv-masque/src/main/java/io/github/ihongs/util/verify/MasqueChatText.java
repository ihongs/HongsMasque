package io.github.ihongs.util.verify;

import io.github.ihongs.HongsException;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.util.Synt;
import java.util.Map;

/**
 * 判断并标记非文本
 * @author hong
 */
public class MasqueChatText extends Rule {

    @Override
    public Object verify(Object value, Veri watch) throws Wrong {
        if (null != value && !"".equals(value)) {
            return  value;
        }

        Map  values = watch.getValues();
        String kind = Synt.declare(values.get("kind"), "text");
        switch(kind) {
            case "link" :
            case "file" :
            case "image":
            case "video":
            case "voice":
                if (value == null || "".equals(value)) {
                try {
                    return "["+ FormSet.getInstance ("masque")
                                        .getEnum ("chat_kind")
                                        .get     ( kind ) +"]";
                } catch ( HongsException ex) {
                  throw   ex.toExemption(  );
                } }
                return value;
            default:
                if (value == null || "".equals(value)) {
                    throw  new  Wrong ( "fore.form.required" );
                }
                return value;
        }
    }

}
