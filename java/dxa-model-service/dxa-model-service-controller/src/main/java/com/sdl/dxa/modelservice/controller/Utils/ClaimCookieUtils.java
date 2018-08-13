package com.sdl.dxa.modelservice.controller.Utils;

import com.tridion.ambientdata.AmbientDataContext;
import com.tridion.ambientdata.claimstore.ClaimStore;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Base64;

/**
 * Utility class which sets up claim store with cookie values
 */
public class ClaimCookieUtils {
    /**
     * Finds cookies that need to be mapped to claimstore values.
     * @param req http request
     */
    public static void setupClaimStore(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) {
            return;
        }
        ClaimStore claimStore = AmbientDataContext.getCurrentClaimStore();
        for (Cookie cooky : cookies) {
            if (cooky.getName().startsWith("taf.")) {
                String value = new String(Base64.getDecoder().decode(cooky.getValue()));
                URI uriKey = URI.create(cooky.getName().replace('.', ':'));
                if (!claimStore.contains(uriKey) && !claimStore.isReadOnly(uriKey)) {
                    claimStore.put(uriKey, value);
                }
            }
        }
    }
}
