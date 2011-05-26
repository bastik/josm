// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Date;

public interface IPrimitive extends Tagged, PrimitiveId {

    boolean isModified();
    void setModified(boolean modified);
    boolean isVisible();
    void setVisible(boolean visible);
    boolean isDeleted();
    void setDeleted(boolean deleted);
    boolean isIncomplete();
    long getId();
    int getVersion();
    void setOsmId(long id, int version);
    User getUser();
    void setUser(User user);
    Date getTimestamp();
    void setTimestamp(Date timestamp);
    boolean isTimestampEmpty();
    int getChangesetId();
    void setChangesetId(int changesetId);

}
