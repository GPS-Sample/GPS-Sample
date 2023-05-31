package edu.gtri.gpssample.constants

enum class Role {
    Undefined,
    Admin,
    Supervisor,
    Enumerator,
    DataCollector
}

object RoleConverter
{
    fun getRole(roleString : String) : Role
    {
        var role : Role = Role.Undefined
        when(roleString)
        {
            Role.Admin.toString() -> role = Role.Admin
            Role.Supervisor.toString() -> role = Role.Supervisor
            Role.Enumerator.toString() -> role = Role.Enumerator
        }
        return role
    }
}