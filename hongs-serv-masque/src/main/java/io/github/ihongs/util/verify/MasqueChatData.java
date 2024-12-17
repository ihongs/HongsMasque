package io.github.ihongs.util.verify;

import io.github.ihongs.Core;
import io.github.ihongs.action.DownPart;
import io.github.ihongs.util.Dist;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;
import io.github.ihongs.util.sketch.Thumb;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import javax.servlet.http.Part;

/**
 * 验证是否许可访问
 * @author hong
 */
public class MasqueChatData extends Rule {

    @Override
    public Object verify(Value watch) throws Wrong {
        Object val  = watch.get();
        Map    vals = watch.getValues();
        String  id  = Synt.declare(vals.get( "id" ),   ""  );
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
                String  x = "";
                String  t = "png";
                String  s = "_sm.png";
                try {
                    Part    part =  toPart  (val);
                    name  = part.getSubmittedFileName();
                    int p = name.lastIndexOf(".");
                    if (p > -1) {
                        x = name.substring  ( p );
                    }
                    part.write (path +x);
                    new Thumb  (path +x)
                        .pick  (300,300)
                        .outputFormat(t)
                        .toFile(path +s);
                } catch (IOException ex) {
                    throw new Wrong (ex, ex.getLocalizedMessage());
                }

                return Dist.toString(Synt.mapOf(
                    "href", href + x,
                    "snap", href + s,
                    "name", name
                ) );
            }
            case "video":
            case "voice":
            case "file" :
            {
                String name = Syno.splitPath ( id );
                String href = Core.SERVER_HREF.get()
                            + Core.SERV_PATH+"/static/upload/masque/"+kind+"/"+name;
                String path = Core.BASE_PATH+"/static/upload/masque/"+kind+"/"+name;
                File   dir  = new File(path).getParentFile();
                if ( ! dir.exists()) {
                       dir.mkdirs();
                }

                // 转移文件
                String  x = "";
                try {
                    Part    part =  toPart  (val);
                    name  = part.getSubmittedFileName();
                    int p = name.lastIndexOf(".");
                    if (p > -1) {
                        x = name.substring  ( p );
                    }
                    part.write (path +x);
                } catch (IOException ex) {
                    throw new Wrong (ex, ex.getLocalizedMessage());
                }

                return Dist.toString(Synt.mapOf(
                    "href", href + x,
                    "snap", "" ,
                    "name", name
                ) );
            }
            case "link" :
                if (val instanceof Map) {
                    return  val;
                }

                return Dist.toString(Synt.mapOf(
                    "href", val,
                    "snap", "" ,
                    "desc", ""
                ) );
            default:
                return "{}";
        }
    }

    private Part toPart(Object val) throws Wrong {
        if (val instanceof Part) {
            return ((Part) val );
        } else {
            String url = val.toString( );
            if (url.contains("./")) {
                throw  new Wrong("@core.file.upload.not.allows");
            }
            if (url.contains( "/")) {
                return new DownPart(url);
            } else {
                String fil = null;
                String tmp = Core.BASE_PATH+"/static/upload/tmp";
                int p = url.indexOf("|");
                if (p > 0) {
                    fil = url.substring(1+p);
                    url = url.substring(0,p);
                }   url = tmp + "/" + url;
                return new DownPart("file:"+url,false).file(fil);
            }
        }

    }

}
