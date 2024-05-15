# API

## Additional error levels
Most cloud logging backends support more error levels than SFL4J. 

This library lets developers add some more details about the seriousness of the errors:

* Tell me tomorrow (default error logging)
    * Handled by devops team during next available work hours
* Interrupt my dinner
    * Handled by devops team if within work hours, otherwise
    * Handled by operations team during wake hours
* Wake me up right now
    * Handled by devops team if within work hours, otherwise
    * Handled by operations team during wake or sleep hours

