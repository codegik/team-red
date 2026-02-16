# Lambda Data Processing Architecture

Lambda architecture is a way of processing massive quantities of data that provides access to batch-processing and stream-processing methods with a hybrid approach.

It has 3 layers:

![lambda](lambda-architecture.png)

### Batch Layer

* Manages the master dataset (raw data) and pre-computes results (batch views) to ensure accuracy.
* Data is fed to batch layer and speed layer simultaneously
* It looks at all the data at once and eventually corrects the data in the speed layer
* ETLs (Extract, Transform, Load) and data warehouse
* predefined schedule (usually once or twice a day)
* Output is a batch view

### Serving Layer

* Input is the batch views from the batch layer, and also near real-time views from the speed layer
* Index the batch views so they can be queried with low latency

### Speed Layer / Stream Layer 

* Processes data in real-time, handling recent data to provide low-latency views.