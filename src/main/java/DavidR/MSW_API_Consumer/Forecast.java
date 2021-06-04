package DavidR.MSW_API_Consumer;

public class Forecast {
	int timestamp;
	int localTimestamp;
	int issueTimestamp;
	int fadedRating;
	int solidRating;
	Swell swell;
	Wind wind;
	Condition condition;
	Charts charts;
}

class Swell {
	double absMinBreakingHeight;
	double absMaxBreakingHeight;
	int probability;
	String unit;
	int minBreakingHeight;
	int maxBreakingHeight;
	Components components;
}

class Components {
	Component combined;
	Component primary;
	Component secondary;
}

class Component {
	int height;
	int period;
	double direction;
	String compassDirection;
}

class Wind {
	int speed;
	int direction;
	String compassDirection;
	int chill;
	int gusts;
	String unit;
}

class Condition {
	int pressure;
	int temperature;
	String weather;
	String unitPressure;
	String unit;
}

class Charts {
	String swell;
	String period;
	String wind;
	String pressure;
	String sst;
}