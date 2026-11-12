package net.dogpoo.api.core.weaver.model;
public record WeaverSocialIdentity(String provider,String subject,String email,String name,String avatarUrl) {
 public WeaverSocialIdentity(String provider,String subject,String email,String name){this(provider,subject,email,name,null);}
}
