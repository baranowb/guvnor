/*
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.guvnor.server.security;

import org.drools.repository.RulesRepository;
import org.drools.repository.security.PermissionManager;
import javax.inject.Inject;
import javax.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Named("org.drools.guvnor.server.security.RoleBasedPermissionStore")
public class RoleBasedPermissionStore implements Serializable {

    @Inject
    public RulesRepository rulesRepository;


    public RoleBasedPermissionStore() {
    }

    public List<RoleBasedPermission> getRoleBasedPermissionsByUserName(
            String userName) {
        PermissionManager permissionManager = new PermissionManager(rulesRepository);
        List<RoleBasedPermission> permissions = new ArrayList<RoleBasedPermission>();
        Map<String, List<String>> perms = permissionManager
                .retrieveUserPermissions(userName);
        for (Map.Entry<String, List<String>> permEntry : perms.entrySet()) {
            resolvePermissionsAndAdd(userName,
                    permissions,
                    permEntry);
        }

        return permissions;
    }

    private void resolvePermissionsAndAdd(String userName,
                                          List<RoleBasedPermission> permissions,
                                          Map.Entry<String, List<String>> permEntry) {
        String roleType = permEntry.getKey();
        if (RoleType.ADMIN.getName().equals(roleType)) {
            permissions.add(new RoleBasedPermission(userName, RoleType.ADMIN.getName(),
                    null, null));
        }

        List<String> permissionsPerRole = permEntry.getValue();
        for (String permissionPerRole : permissionsPerRole) {
            if (permissionPerRole.startsWith("package=")) {
                String packageName = permissionPerRole.substring("package="
                        .length());
                permissions.add(new RoleBasedPermission(userName, roleType,
                        packageName, null));
            } else if (permissionPerRole.startsWith("category=")) {
                String categoryPath = permissionPerRole
                        .substring("category=".length());
                permissions.add(new RoleBasedPermission(userName, roleType,
                        null, categoryPath));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void addRoleBasedPermissionForTesting(String userName, RoleBasedPermission rbp) {
        PermissionManager permissionManager = new PermissionManager(rulesRepository);
        Map<String, List<String>> perms = permissionManager
                .retrieveUserPermissions(userName);
        Object permissionsPerRole = perms.get(rbp.getRole());
        if (permissionsPerRole != null) {
            if (rbp.getPackageName() != null) {
                ((List<String>) permissionsPerRole).add("package="
                        + rbp.getPackageName());
            } else if (rbp.getCategoryPath() != null) {
                ((List<String>) permissionsPerRole).add("category="
                        + rbp.getPackageName());
            }

        } else {
            List<String> perm = new ArrayList<String>();
            if (rbp.getPackageName() != null) {
                perm.add("package=" + rbp.getPackageName());
            } else if (rbp.getCategoryPath() != null) {
                perm.add("category=" + rbp.getCategoryPath());
            }
            perms.put(rbp.getRole(), perm);
        }

        permissionManager.updateUserPermissions(userName, perms);
    }

}
