package retrofit;

import java.util.ArrayList;
import java.util.List;

/**
 * Records methods called against it as a RequestFacade and replays them when called as a
 * RequestInterceptor.
 */
final class RequestInterceptorTape implements RequestInterceptor.RequestFacade, RequestInterceptor {

  private final List<CommandWithParams> tape = new ArrayList<CommandWithParams>();

  @Override public void addHeader(String name, String value) {
    tape.add(new CommandWithParams(Command.ADD_HEADER, name, value));
  }

  @Override public void addPathParam(String name, String value) {
    tape.add(new CommandWithParams(Command.ADD_PATH_PARAM, name, value));
  }

  @Override public void addEncodedPathParam(String name, String value) {
    tape.add(new CommandWithParams(Command.ADD_ENCODED_PATH_PARAM, name, value));
  }

  @Override public void addQueryParam(String name, String value) {
    tape.add(new CommandWithParams(Command.ADD_QUERY_PARAM, name, value));
  }

  @Override public void addEncodedQueryParam(String name, String value) {
    tape.add(new CommandWithParams(Command.ADD_ENCODED_QUERY_PARAM, name, value));
  }

  @Override public void intercept(RequestFacade request) {
    for (CommandWithParams cwp : tape) {
      cwp.command.intercept(request, cwp.name, cwp.value);
    }
  }

  private enum Command {
    ADD_HEADER {
      @Override
      public void intercept(RequestFacade facade, String name, String value) {
        facade.addHeader(name, value);
      }
    },
    ADD_PATH_PARAM {
      @Override
      public void intercept(RequestFacade facade, String name, String value) {
        facade.addPathParam(name, value);
      }
    },
    ADD_ENCODED_PATH_PARAM {
      @Override
      public void intercept(RequestFacade facade, String name, String value) {
        facade.addEncodedPathParam(name, value);
      }
    },
    ADD_QUERY_PARAM {
      @Override
      public void intercept(RequestFacade facade, String name, String value) {
        facade.addQueryParam(name, value);
      }
    },
    ADD_ENCODED_QUERY_PARAM {
      @Override
      public void intercept(RequestFacade facade, String name, String value) {
        facade.addEncodedQueryParam(name, value);
      }
    };

    abstract void intercept(RequestFacade facade, String name, String value);
  }

  private static final class CommandWithParams {
    final Command command;
    final String name;
    final String value;

    CommandWithParams(Command command, String name, String value) {
      this.command = command;
      this.name = name;
      this.value = value;
    }
  }
}
