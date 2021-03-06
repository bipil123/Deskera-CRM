/*
 * Copyright (C) 2012  Krawler Information Systems Pvt Ltd
 * All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package com.krawler.crm.database.tables;
// Generated Jun 19, 2009 11:17:38 AM by Hibernate Tools 3.2.1.GA



/**
 * Myprofile generated by hbm2java
 */
public class Myprofile  implements java.io.Serializable {


     private String userid;
     private String fname;
     private String lname;
     private String emailid;
     private String address;
     private String designation;
     private String contactno;
     private String aboutuser;
     private String fax;
     private String altcontactno;
     private String panno;
     private String ssnno;
     private String company;

    public Myprofile() {
    }

    public Myprofile(String userid, String fname, String lname, String emailid, String address, String designation, String contactno, String aboutuser, String fax, String altcontactno, String panno, String ssnno, String company) {
       this.userid = userid;
       this.fname = fname;
       this.lname = lname;
       this.emailid = emailid;
       this.address = address;
       this.designation = designation;
       this.contactno = contactno;
       this.aboutuser = aboutuser;
       this.fax = fax;
       this.altcontactno = altcontactno;
       this.panno = panno;
       this.ssnno = ssnno;
       this.company = company;
    }
   
    public String getUserid() {
        return this.userid;
    }
    
    public void setUserid(String userid) {
        this.userid = userid;
    }
    public String getFname() {
        return this.fname;
    }
    
    public void setFname(String fname) {
        this.fname = fname;
    }
    public String getLname() {
        return this.lname;
    }
    
    public void setLname(String lname) {
        this.lname = lname;
    }
    public String getEmailid() {
        return this.emailid;
    }
    
    public void setEmailid(String emailid) {
        this.emailid = emailid;
    }
    public String getAddress() {
        return this.address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    public String getDesignation() {
        return this.designation;
    }
    
    public void setDesignation(String designation) {
        this.designation = designation;
    }
    public String getContactno() {
        return this.contactno;
    }
    
    public void setContactno(String contactno) {
        this.contactno = contactno;
    }
    public String getAboutuser() {
        return this.aboutuser;
    }
    
    public void setAboutuser(String aboutuser) {
        this.aboutuser = aboutuser;
    }
    public String getFax() {
        return this.fax;
    }
    
    public void setFax(String fax) {
        this.fax = fax;
    }
    public String getAltcontactno() {
        return this.altcontactno;
    }
    
    public void setAltcontactno(String altcontactno) {
        this.altcontactno = altcontactno;
    }
    public String getPanno() {
        return this.panno;
    }
    
    public void setPanno(String panno) {
        this.panno = panno;
    }
    public String getSsnno() {
        return this.ssnno;
    }
    
    public void setSsnno(String ssnno) {
        this.ssnno = ssnno;
    }
    public String getCompany() {
        return this.company;
    }
    
    public void setCompany(String company) {
        this.company = company;
    }




}


