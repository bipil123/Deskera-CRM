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
package com.krawler.crm.users.dao;

import java.util.List;

import com.krawler.common.admin.User;
import com.krawler.dao.BaseDAO;

public class UserDAOImpl extends BaseDAO implements UserDAO
{

    @SuppressWarnings("unchecked")
    public List<User> getAllUsers()
    {
        return executeQuery("from Users");
    }

    public void saveUser(User user)
    {
        saveOrUpdate(user);
    }

    public void saveUsers(List<User> users)
    {
        if (users != null)
        {
            for (User user: users)
            {
                saveUser(user);
            }
        }
    }

}
