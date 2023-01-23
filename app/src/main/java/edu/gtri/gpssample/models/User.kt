package edu.gtri.gpssample.models

import edu.gtri.gpssample.constants.Role

class User
{
    var id: Int = 0;
    var name: String = "";
    var pin: Int = 0;
    var role: Role = Role.Undefined
    var recoveryQuestion: String = "";
    var recoveryAnswer: String = "";
    var isOnline: Boolean = false;
}