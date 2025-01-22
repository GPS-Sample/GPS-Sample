/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.constants

enum class Role(val value: String) {
    Undefined("Undefined"),
    Admin("Admin"),
    Supervisor("Supervisor"),
    Enumerator("Enumerator"),
    DataCollector("DataCollector")
}

object RoleConverter
{
    fun getRole(roleString : String) : Role
    {
        var role : Role = Role.Undefined
        when(roleString)
        {
            Role.Admin.value -> role = Role.Admin
            Role.Supervisor.value -> role = Role.Supervisor
            Role.Enumerator.value -> role = Role.Enumerator
        }
        return role
    }
}