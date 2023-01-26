package edu.gtri.gpssample.models

import edu.gtri.gpssample.constants.FieldType

class Field
{
    var id: Int = -1
    var studyId: Int = -1
    var name: String = ""
    var type: FieldType = FieldType.Text
    var pii: Boolean = false
    var required: Boolean = false
    var integerOnly: Boolean = false
    var date: Boolean = false
    var time: Boolean = false
    var option1: String = ""
    var option2: String = ""
    var option3: String = ""
    var option4: String = ""
}