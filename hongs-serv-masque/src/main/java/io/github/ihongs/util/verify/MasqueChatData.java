package io.github.ihongs.util.verify;

import io.github.ihongs.Core;
import io.github.ihongs.util.Dist;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * 验证是否许可访问
 * @author hong
 */
public class MasqueChatData extends Rule {

    @Override
    public Object verify(Value watch) throws Wrong {
        Object val  = watch.get();
        Map    vals = watch.getValues();
        String  id  = Synt.declare(vals.get( "id" ), ""/**/);
        String kind = Synt.declare(vals.get("kind"), "text");
        switch(kind) {
            case "image":
            {
                String name = Syno.splitPath ( id );
                String href = Core.SERVER_HREF.get()
                            + Core.SERV_PATH+"/static/upload/masque/"+kind+"/"+name;
                String path = Core.BASE_PATH+"/static/upload/masque/"+kind+"/"+name;
                File   dir  = new File(path).getParentFile();
                if ( ! dir.exists()) {
                       dir.mkdirs();
                }

                // 取缩略图
                try {
                    io.github.ihongs.util.sketch.Thumb th = new io.github.ihongs.util.sketch.Thumb(Core.BASE_PATH+"/static/upload/tmp/"+val);
                    th.make().scale(1).outputFormat("png").toFile(path +    ".png");
                    th.pick(300 , 300).outputFormat("png").toFile(path + "_sm.png");
                } catch (IOException ex) {
                    throw new Wrong (ex, ex.getLocalizedMessage());
                }

                return Dist.toString(Synt.mapOf(
                    "href", href +    ".png",
                    "snap", href + "_sm.png",
                    "desc", ""
                ) );
            }
            case "video":
            case "voice":
            case "file" :
            {
                String name = Syno.splitPath (id);
                String path = Core.BASE_PATH+"/static/upload/masque/"+kind+"/"+name;
                String href = Core.SERV_PATH+"/static/upload/masque/"+kind+"/"+name;
                       href = Core.SERVER_HREF.get() + href ;
                File   dir  = new File(path).getParentFile();
                if ( ! dir.exists()) {
                       dir.mkdirs();
                }

                // 转移文件
                name = (String) val;
                int pos  = name.lastIndexOf(".");
                if (pos != -1 ) {
                    name = name.substring  (pos);
                } else {
                    name =  "" ;
                }
                File df = new File (path + name);
                File sf = new File (Core.BASE_PATH + "/static/upload/tmp/" + val);
                sf.renameTo(df);

                return Dist.toString(Synt.mapOf(
                    "href", href + name,
                    "snap", "",
                    "desc", ""
                ) );
            }
            case "link" :
                if (val instanceof Map) {
                    return  val;
                }

                return Dist.toString(Synt.mapOf("href", val,
                    "snap", "",
                    "desc", ""
                ) );
            default:
                return "{}";
        }
    }

}
