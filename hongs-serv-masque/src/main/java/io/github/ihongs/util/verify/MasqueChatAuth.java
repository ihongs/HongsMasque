package io.github.ihongs.util.verify;

import io.github.ihongs.CruxException;
import io.github.ihongs.db.DB;
import io.github.ihongs.dh.Roster;
import io.github.ihongs.util.Digest;
import io.github.ihongs.util.Synt;
import java.util.Map;

/**
 * 验证是否许可访问
 * @author hong
 */
public class MasqueChatAuth extends Rule {

    @Override
    public Object verify(Value watch) throws Wrong {
        Object val = watch.get();
        Map values = watch.getValues();
        Map cleans = watch.getCleans();
        String sid = (String) values.get("site_id");
        String mid = (String) values.get("mate_id");
        String rid = (String) values.get("room_id");
        String tok = (String) val ;
        String tsk ;
        Map    row ;

        try {
            row = DB.getInstance("masque")
                .with  ("site")
                .select("`sk`")
                .filter("`id`=? AND `state`=1",sid)
                .getOne();
        } catch ( CruxException e) {
            throw e.toExemption( );
        }
        if (row.isEmpty()) {
            throw new Wrong("@masque:core.masque.wrong.sites");
        }

        // 原始 token
        tsk = Synt.asString(row.get("sk"));
        if (tok.equals(tsk)) {
            cleans.put( "token_level", 1 );
            return val;
        }

        // 临时 token
        tsk = Digest.md5(sid +"/"+ mid +"/"+ rid +"/"+ tsk );
        tok = Synt.asString(Roster.get("masque.auth."+ tok));
        if (tok.equals(tsk)) {
            cleans.put( "token_level", 2 );
            return val;
        }

        /**/throw new Wrong("@masque:core.masque.wrong.token");
    }

}
