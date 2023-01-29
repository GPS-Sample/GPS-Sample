package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.Role

class User
{
    var id: Int = -1
    var uuid: String = ""
    var name: String = ""
    var pin: Int = 0
    var role: Role = Role.Undefined
    var recoveryQuestion: String = ""
    var recoveryAnswer: String = ""
    var isOnline: Boolean = false
}