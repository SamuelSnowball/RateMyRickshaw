# RateMyRickshaw 🛺

AI-powered rickshaw number plate detection and rating system for India's auto-rickshaws.

## Overview

When I was travelling in India I used to hate bartering with Rickshaw / Tuk-tuk drivers about how much the price was going to be. Usually they wave you into their vehicle, drop you off - and then ask for an outrageous price. At the time Uber didn’t accept card payments (what’s the point of this app again..?) and so you had to pay cash everywhere. CONVIENTLY, these drivers never “had change”, another of my frustrations! Anyway, the idea of this system was to give passengers the ability to quickly scan a rickshaw number plate, which would pull back the user ratings of that driver. Giving the potential passenger a bit more insight into their next journey. 

It's essentially a worse version of an uber feature, though at the time I had this idea there was no Uber avaliable in India (which took card..).

The idea was for a system to give passengers the ability to quickly scan a rickshaw number plate and view driver ratings, providing insight before the journey begins.

## 🚀 Live Demo

**Production URL:** [https://ratemyrickshaw.snowballsjourney.com/](https://ratemyrickshaw.snowballsjourney.com/)

## Architecture

- **Frontend:** React SPA hosted on S3 + CloudFront (HTTPS)
- **Backend:** AWS Lambda (Java 21 + Quarkus)
- **API:** API Gateway REST API
- **AI/ML:** AWS Rekognition for OCR and number plate detection
- **Infrastructure:** AWS SAM (CloudFormation)

## ✅ Features Implemented

- ✅ Image upload (file or URL)
- ✅ Number plate detection using AWS Rekognition
- ✅ Indian number plate validation
- ✅ Example images (good and bad detection scenarios)

## Features Planned

- [ ] Database integration (DynamoDB)
- [ ] Driver ratings and reviews system
- [ ] Historical rating data
- [ ] User authentication
- [ ] Mobile app version


## Tech Stack

**Frontend:**
- React
- JavaScript
- CSS

**Backend:**
- Java 21
- Quarkus
- Maven
- AWS Lambda
- AWS Rekognition

**Infrastructure:**
- AWS SAM
- CloudFormation
- S3
- CloudFront
- API Gateway
- Route53
- ACM (SSL/TLS certificates)