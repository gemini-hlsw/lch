package edu.gemini.lch.model;

import javax.persistence.*;

/**
 * Object to model files that belong to events (e.g. emails, PAM and PRM files etc.).
 */
@Entity
@Table(name = "lch_files")
public class EventFile implements Comparable<EventFile> {

    public enum Type {
        EMAIL_HTML,
        EMAIL,
        PAM,
        PRM
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "folder_id")
    private Long folderId;

    @Column(name = "file_id")
    private Long fileId;

    @Enumerated(EnumType.STRING)
    @Column
    private Type type;


    @Column
    private String name;

    @Column
    private String content;

    public EventFile(Type type, String name, String content) {
        this(0, 0, type, name, content);
    }

    public EventFile(long folderId, long fileId, Type type, String name, String content) {
        this.folderId = folderId;
        this.fileId   = fileId;
        this.type     = type;
        this.name     = name;
        this.content  = content;
    }

    public Long getFolderID() { return folderId; }

    public Long getFileID() { return fileId; }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    /** {@inheritDoc} */
    // NOTE: compareTo should be consistent with order defined in @OrderBy in LaserRunEvent for EventFile objects.
    @Override
    public int compareTo(EventFile other) {
        int typeComp = type.compareTo(other.type);
        if (typeComp == 0) {
            return name.compareTo(other.name);
        } else {
            return typeComp;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return type.hashCode() + name.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof EventFile)) return false;
        EventFile other = (EventFile) o;
        if (!type.equals(other.type)) return false;
        return name.equals(other.name);
    }

    // empty constructor needed for hibernate
    public EventFile() {}

}
