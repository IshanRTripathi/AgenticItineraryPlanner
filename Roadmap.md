1. UI
   a. User is on homepage and clicks on "Get Started" button
   b. User is in the wizard page and clicks on create my itinerary button 
   c. Different agents start working based on the provided input
   d. Once all processing is done, user sees the page where they can view and edit their itinerary
   e. The same page has full details like flights, hotels, activities, dates, weather and news. the stippl page
   f. Users can edit any part of the itinerary
   g. Users can see all itineraries created so far in the dashboard and click on any of them to view/edit
   h. All data is saved in the backend and can be retrieved later using the controllers present

2. Backend 
   a. Create my itinerary controller starts all agents in the backend that work in parallel or series based on their specific settings
   b. Once all agents are done, the final itinerary is created and saved in the database
   c. There are controllers to get all itineraries and get a specific itinerary by id
   d. There are controllers to update any part of the itinerary like flights, hotels, activities etc.

3. Agents
   a. There are different agents for different tasks like flight search, hotel search, activity search, weather fetch, news fetch etc.
   b. Each agent has its own specific settings in their java files and can work in parallel or series based on the requirements
   c. All agents take input from the user and provide output in a structured json format provided that can be used to create the final itinerary
   d. Each response must be 100% compatible with the data models in both backend and UI
   e. All agents must handle errors gracefully and provide meaningful error messages in case of failures

4. Data Models
   a. There are data models for itinerary, flights, hotels, activities, weather, news etc.
   b. Each data model has its own specific fields
   c. All data models are used in both backend and UI to ensure compatibility and consistency, no difference in field names or structure is allowed
   d. Any changes to the data models must be reflected in both backend and UI always
   e. Start with very basic set of fields that are must for any itinerary and can be extended later as needed




DROP TABLE TRANSPORTATION CASCADE;
DROP TABLE  ACCOMMODATIONS CASCADE;
DROP TABLE  ACCOMMODATION_AMENITIES CASCADE;
DROP TABLE  ACTIVITIES CASCADE;
DROP TABLE  BOOKINGS CASCADE;
DROP TABLE  ITINERARIES CASCADE;
DROP TABLE  ITINERARY_CONSTRAINTS CASCADE;
DROP TABLE  ITINERARY_DAYS CASCADE;
DROP TABLE  ITINERARY_INTERESTS CASCADE;
DROP TABLE  LOCATIONS CASCADE;
DROP TABLE  MEALS CASCADE;
DROP TABLE  PRICES CASCADE;

